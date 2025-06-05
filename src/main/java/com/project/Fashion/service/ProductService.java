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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
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
// Removed: import java.util.stream.Collectors; // Not needed if mapping Page directly

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

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
            int page,
            int size,
            String category,
            String searchTerm,
            Float minPrice,
            Float maxPrice,
            Float minRating,
            String sortBy,
            String sortDir) {
        log.info("Fetching products from DB: page={}, size={}, category={}, searchTerm={}, minPrice={}, maxPrice={}, minRating={}, sortBy={}, sortDir={}",
                page, size, category, searchTerm, minPrice, maxPrice, minRating, sortBy, sortDir);
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDir != null && sortDir.equalsIgnoreCase("DESC")) {
            direction = Sort.Direction.DESC;
        }

        String sortProperty = StringUtils.hasText(sortBy) ? sortBy : "name";
        List<String> validSortProperties = List.of("name", "price", "averageRating", "id");
        if (!validSortProperties.contains(sortProperty)) {
            sortProperty = "name";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(category)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase()));
            }
            if (StringUtils.hasText(searchTerm)) {
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + searchTerm.toLowerCase() + "%");
                Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + searchTerm.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(namePredicate, descriptionPredicate));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minRating != null && minRating > 0) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::toDto);
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        log.info("Fetching product from DB with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    @Caching(evict = {
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "productCategories", allEntries = true),
            @CacheEvict(value = "productPriceRange", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true) // Evict seller-specific cache
    })
    public ProductDto createProduct(ProductDto productDto) {
        User authenticatedSeller = getCurrentAuthenticatedUser();
        if (!"SELLER".equalsIgnoreCase(authenticatedSeller.getRole())) {
            throw new AccessDeniedException("Only users with SELLER role can create products.");
        }
        if (productDto.getSellerId() != null && !productDto.getSellerId().equals(authenticatedSeller.getId())) {
            log.warn("Seller ID in DTO ({}) does not match authenticated seller ID ({}). Using authenticated seller.",
                    productDto.getSellerId(), authenticatedSeller.getId());
        }
        productDto.setSellerId(authenticatedSeller.getId());
        Product product = productMapper.toEntity(productDto);
        product.setSeller(authenticatedSeller);
        product.setAverageRating(0.0f);
        product.setNumOfReviews(0);
        Product savedProduct = productRepository.save(product);
        log.info("Product {} created by seller {}. Evicting relevant caches.", savedProduct.getId(), authenticatedSeller.getEmail());
        return productMapper.toDto(savedProduct);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "productCategories", allEntries = true),
            @CacheEvict(value = "productPriceRange", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true) // Evict seller-specific cache
    })
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        User authenticatedUser = getCurrentAuthenticatedUser();
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        checkProductOwnership(existingProduct, authenticatedUser);
        if (StringUtils.hasText(productDto.getName())) existingProduct.setName(productDto.getName());
        if (StringUtils.hasText(productDto.getDescription())) existingProduct.setDescription(productDto.getDescription());
        if (productDto.getPrice() >= 0) existingProduct.setPrice(productDto.getPrice());
        else throw new IllegalArgumentException("Price cannot be negative.");
        if (StringUtils.hasText(productDto.getCategory()) && !existingProduct.getCategory().equalsIgnoreCase(productDto.getCategory())) {
            existingProduct.setCategory(productDto.getCategory());
        }
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product {} updated by owner {}. Evicting relevant caches.", updatedProduct.getId(), authenticatedUser.getEmail());
        return productMapper.toDto(updatedProduct);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "productCategories", allEntries = true),
            @CacheEvict(value = "productPriceRange", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true) // Evict seller-specific cache
    })
    public void deleteProduct(Long id) {
        User authenticatedUser = getCurrentAuthenticatedUser();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        boolean isAdmin = authenticatedUser.getRole().equalsIgnoreCase("ADMIN");
        if (isAdmin) log.info("Admin {} is deleting product {}", authenticatedUser.getEmail(), id);
        else if ("SELLER".equalsIgnoreCase(authenticatedUser.getRole())) {
            checkProductOwnership(product, authenticatedUser);
            log.info("Seller {} is deleting their own product {}", authenticatedUser.getEmail(), id);
        } else {
            log.warn("Access Denied: User {} (Role: {}) attempted to delete product {} without sufficient privileges.",
                    authenticatedUser.getEmail(), authenticatedUser.getRole(), id);
            throw new AccessDeniedException("Access Denied: You do not have permission to delete this product.");
        }
        productRepository.deleteById(id);
        log.info("Evicting relevant caches due to deletion of product {}.", id);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "productsList", allEntries = true),
            @CacheEvict(value = "sellerProducts", allEntries = true) // Evict seller-specific cache
    })
    public ProductDto addImageToProduct(Long productId, MultipartFile file) {
        User authenticatedSeller = getCurrentAuthenticatedUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));
        checkProductOwnership(product, authenticatedSeller);
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String originalFilename = file.getOriginalFilename();
            String filename = "product_" + productId + "_" + System.currentTimeMillis() + "_" +
                    (StringUtils.hasText(originalFilename) ? StringUtils.cleanPath(originalFilename).replaceAll("\\s+", "_") : "image.png");
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, REPLACE_EXISTING);
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/products/image/")
                    .path(filename)
                    .toUriString();
            product.setPhotoUrl(fileUrl);
            Product savedProduct = productRepository.save(product);
            log.info("Image added to product {} by owner {}. Evicting relevant caches.", savedProduct.getId(), authenticatedSeller.getEmail());
            return productMapper.toDto(savedProduct);
        } catch (IOException e) {
            log.error("Failed to store file for product id {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to store file. " + e.getMessage(), e);
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

    /**
     * Retrieves a paginated list of products for the currently authenticated seller.
     * @param pageable Pagination and sorting information.
     * @return A Page of ProductDto.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "sellerProducts", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString(), T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()}")
    public Page<ProductDto> getProductsByAuthenticatedSeller(Pageable pageable) {
        User authenticatedSeller = getCurrentAuthenticatedUser();
        if (!"SELLER".equalsIgnoreCase(authenticatedSeller.getRole())) {
            log.warn("User {} with role {} attempted to access seller-specific product list.", authenticatedSeller.getEmail(), authenticatedSeller.getRole());
            throw new AccessDeniedException("Only users with SELLER role can access this resource.");
        }
        log.info("Fetching products for authenticated seller: {} with pagination: {}", authenticatedSeller.getEmail(), pageable);
        Page<Product> productsPage = productRepository.findBySellerId(authenticatedSeller.getId(), pageable);
        return productsPage.map(productMapper::toDto); // Map Page<Product> to Page<ProductDto>
    }
}
