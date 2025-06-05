package com.project.Fashion.service;

import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.dto.ProductPriceRangeDto;
import com.project.Fashion.config.mappers.ProductMapper;
import com.project.Fashion.exception.exceptions.ProductNotFoundException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.config.RdfConfigProperties;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final SparqlQueryService sparqlQueryService;
    private final RdfConfigProperties rdfConfigProperties;
    private final RdfConversionService rdfConversionService; // Added RdfConversionService

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/products";

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        } else {
            throw new AccessDeniedException("Invalid principal type.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found in database. Email: " + email));
    }

    private void checkProductOwnership(Product product, User seller) {
        if (!"SELLER".equalsIgnoreCase(seller.getRole())) {
            throw new AccessDeniedException("User performing the action is not a SELLER.");
        }
        if (product.getSeller() == null || !product.getSeller().getId().equals(seller.getId())) {
            log.warn("Access Denied: User {} (Role: {}) attempted to modify product {} owned by {}",
                    seller.getEmail(), seller.getRole(), product.getId(),
                    product.getSeller() != null ? product.getSeller().getId() : "UNKNOWN_SELLER");
            throw new AccessDeniedException("Access Denied: You do not own this product.");
        }
    }

    @Cacheable(value = "productsList", key = "{#page, #size, #category, #searchTerm, #minPrice, #maxPrice, #minRating, #sortBy, #sortDir}")
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(
            int page, int size, String category, String searchTerm,
            Float minPrice, Float maxPrice, Float minRating, String sortBy, String sortDir) {
        log.info("Fetching products from DB: page={}, size={}, category={}, searchTerm={}, minPrice={}, maxPrice={}, minRating={}, sortBy={}, sortDir={}",
                page, size, category, searchTerm, minPrice, maxPrice, minRating, sortBy, sortDir);
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDir != null && sortDir.equalsIgnoreCase("DESC")) {
            direction = Sort.Direction.DESC;
        }
        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "name";
        List<String> validSortProperties = List.of("name", "price", "averageRating", "id");
        if (!validSortProperties.contains(sortProperty)) sortProperty = "name";

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(category)) predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            if (StringUtils.hasText(searchTerm)) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + searchTerm.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("description")), "%" + searchTerm.toLowerCase() + "%")
                ));
            }
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (minRating != null && minRating > 0) predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            // Eager fetch reviews and seller to ensure they are available for RDF conversion
            // if RDF conversion happens in a different transaction or outside a session.
            // For this method, it's readOnly, so not strictly needed here if not converting.
            // query.distinct(true);
            // root.fetch("reviews", jakarta.persistence.criteria.JoinType.LEFT);
            // root.fetch("seller", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::toDto);
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        log.info("Fetching product from DB with id: {}", id);
        // Fetch with associations for RDF conversion if this DTO were directly converted
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    // Helper to fetch a product fully for RDF conversion (including reviews and seller)
    // This ensures lazy-loaded collections are available.
    private Product getFullyLoadedProduct(Long productId) {
        // Using a custom query or findById and then initialize collections if necessary.
        // For simplicity, ensure the calling context for RDF updates is transactional.
        // If ProductService methods below are @Transactional, lazy loading should work.
        // Otherwise, we'd need a specific query here.
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId + " for RDF update."));
    }


    @Caching(evict = {
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "productCategories", allEntries = true),
            @CacheEvict(value = "productPriceRange", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true),
            @CacheEvict(value = "products", allEntries = true) // Evict single product cache too
    })
    public ProductDto createProduct(ProductDto productDto) {
        User authenticatedSeller = getCurrentAuthenticatedUser();
        if (!"SELLER".equalsIgnoreCase(authenticatedSeller.getRole())) {
            throw new AccessDeniedException("Only SELLERs can create products.");
        }
        Product product = productMapper.toEntity(productDto);
        product.setSeller(authenticatedSeller);
        product.setAverageRating(0.0f);
        product.setNumOfReviews(0);
        Product savedProduct = productRepository.save(product);
        log.info("Product {} created by seller {}. Evicting relevant caches.", savedProduct.getId(), authenticatedSeller.getEmail());

        // Update RDF store
        // Fetch the fully loaded product to ensure all associations (like empty reviews list) are present for RDF
        Product fullyLoadedProduct = getFullyLoadedProduct(savedProduct.getId());
        rdfConversionService.addOrUpdateProductInRdfStore(fullyLoadedProduct);

        return productMapper.toDto(savedProduct);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "productCategories", allEntries = true),
            @CacheEvict(value = "productPriceRange", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true)
    })
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        User authenticatedUser = getCurrentAuthenticatedUser();
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        checkProductOwnership(existingProduct, authenticatedUser);

        if (StringUtils.hasText(productDto.getName())) existingProduct.setName(productDto.getName());
        if (StringUtils.hasText(productDto.getDescription())) existingProduct.setDescription(productDto.getDescription());
        if (productDto.getPrice() != null && productDto.getPrice() >= 0) existingProduct.setPrice(productDto.getPrice());
        else if (productDto.getPrice() != null && productDto.getPrice() < 0) throw new IllegalArgumentException("Price cannot be negative.");
        if (StringUtils.hasText(productDto.getCategory()) && !existingProduct.getCategory().equalsIgnoreCase(productDto.getCategory())) {
            existingProduct.setCategory(productDto.getCategory());
        }
        // Note: PhotoURL is managed by addImageToProduct. Seller, AvgRating, NumReviews are managed internally.

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product {} updated by owner {}. Evicting relevant caches.", updatedProduct.getId(), authenticatedUser.getEmail());

        // Update RDF store
        Product fullyLoadedProduct = getFullyLoadedProduct(updatedProduct.getId()); // Fetch with reviews
        rdfConversionService.addOrUpdateProductInRdfStore(fullyLoadedProduct);

        return productMapper.toDto(updatedProduct);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "productCategories", allEntries = true),
            @CacheEvict(value = "productPriceRange", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true)
    })
    public void deleteProduct(Long id) {
        User authUser = getCurrentAuthenticatedUser();
        // Check existence first before attempting to remove from RDF
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with id: " + id + " for deletion.");
        }
        // No need to load the full product if we are just deleting by ID from RDF store
        rdfConversionService.removeProductFromRdfStore(id);

        // Proceed with relational DB deletion
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("P_NF:" + id)); // Should not happen due to above check
        if (authUser.getRole().equalsIgnoreCase("ADMIN")) log.info("Admin {} deleting product {}", authUser.getEmail(), id);
        else if ("SELLER".equalsIgnoreCase(authUser.getRole())) {
            checkProductOwnership(product, authUser);
            log.info("Seller {} deleting own product {}", authUser.getEmail(), id);
        } else throw new AccessDeniedException("No permission to delete product " + id);

        productRepository.deleteById(id);
        log.info("Evicting caches for deleted product {}", id);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true)
    })
    public ProductDto addImageToProduct(Long productId, MultipartFile file) {
        User authSeller = getCurrentAuthenticatedUser();
        Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException("P_NF:" + productId));
        checkProductOwnership(product, authSeller);
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            String originalFilename = file.getOriginalFilename();
            String filename = "product_" + productId + "_" + System.currentTimeMillis() + "_" +
                    (StringUtils.hasText(originalFilename) ? StringUtils.cleanPath(originalFilename).replaceAll("\\s+", "_") : "image.png");
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, REPLACE_EXISTING);
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/products/image/").path(filename).toUriString();
            product.setPhotoUrl(fileUrl);
            Product savedProduct = productRepository.save(product);
            log.info("Image for product {} added by {}. Caches evicted.", savedProduct.getId(), authSeller.getEmail());

            // Update RDF store
            Product fullyLoadedProduct = getFullyLoadedProduct(savedProduct.getId());
            rdfConversionService.addOrUpdateProductInRdfStore(fullyLoadedProduct);

            return productMapper.toDto(savedProduct);
        } catch (IOException e) {
            log.error("File storage failed for product {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("File storage failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable("productCategories")
    public List<String> getDistinctCategories() {
        log.info("Fetching distinct categories from database.");
        List<String> categories = productRepository.findDistinctCategories();
        return categories != null ? categories : Collections.emptyList();
    }

    @Transactional(readOnly = true)
    @Cacheable("productPriceRange")
    public ProductPriceRangeDto getProductPriceRange() {
        log.info("Fetching product price range from database.");
        Float minPrice = productRepository.findMinPrice();
        Float maxPrice = productRepository.findMaxPrice();
        return new ProductPriceRangeDto(minPrice != null ? minPrice : 0.0f, maxPrice != null ? maxPrice : 0.0f);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sellerProducts", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString(), T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()}")
    public Page<ProductDto> getProductsByAuthenticatedSeller(Pageable pageable) {
        User authSeller = getCurrentAuthenticatedUser();
        if (!"SELLER".equalsIgnoreCase(authSeller.getRole())) {
            log.warn("User {} (role {}) attempted to access seller products.", authSeller.getEmail(), authSeller.getRole());
            throw new AccessDeniedException("Only SELLERs can access this resource.");
        }
        log.info("Fetching products for seller: {} with pagination: {}", authSeller.getEmail(), pageable);
        Page<Product> productsPage = productRepository.findBySellerId(authSeller.getId(), pageable);
        return productsPage.map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> findProductsBySemanticSearch(String categoryName, String descriptionKeyword, Pageable pageable) {
        log.info("Performing optimized semantic product search with category: '{}', keyword: '{}', pageable: {}",
                categoryName, descriptionKeyword, pageable);

        long totalElements = sparqlQueryService.countTotalSemanticSearchResults(categoryName, descriptionKeyword);
        if (totalElements == 0) {
            log.info("No products found from semantic search criteria (count is 0).");
            return Page.empty(pageable);
        }

        List<Map<String, String>> sparqlResultsForPage = sparqlQueryService.searchProductsSemantic(categoryName, descriptionKeyword, pageable);
        if (sparqlResultsForPage.isEmpty() && totalElements > 0) {
            log.info("No products found from paginated semantic search for the current page (page number might be too high).");
            return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
        }

        String productUriPrefix = rdfConfigProperties.getProductUriPrefix();
        List<Long> productIdsOnPage = sparqlResultsForPage.stream()
                .map(resultMap -> resultMap.get("product"))
                .filter(uri -> uri != null && uri.startsWith(productUriPrefix))
                .map(uri -> {
                    try { return Long.parseLong(uri.substring(productUriPrefix.length())); }
                    catch (NumberFormatException e) { log.warn("Could not parse product ID from URI: {}", uri); return null; }
                })
                .filter(id -> id != null).distinct().collect(Collectors.toList());

        if (productIdsOnPage.isEmpty()) {
            log.info("No valid product IDs extracted from paginated semantic search results for the current page.");
            return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
        }
        log.debug("Product IDs from paginated semantic search (current page): {}", productIdsOnPage);

        List<Product> productsOnPage = productRepository.findAllById(productIdsOnPage);
        Map<Long, Product> productMap = productsOnPage.stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<ProductDto> productDtosForPage = productIdsOnPage.stream()
                .map(productMap::get)
                .filter(p -> p != null)
                .map(productMapper::toDto)
                .collect(Collectors.toList());

        log.info("Returning {} products for page {} (size {}) from semantic search. Total matching elements: {}.",
                productDtosForPage.size(), pageable.getPageNumber(), pageable.getPageSize(), totalElements);
        return new PageImpl<>(productDtosForPage, pageable, totalElements);
    }
}