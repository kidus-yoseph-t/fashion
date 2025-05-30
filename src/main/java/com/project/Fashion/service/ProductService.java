package com.project.Fashion.service;

import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.config.mappers.ProductMapper;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/products";

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
            if (minRating != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::toDto);
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    public ProductDto createProduct(ProductDto productDto) {
        if (productDto.getSellerId() == null) {
            throw new RuntimeException("Seller ID cannot be null when creating a product.");
        }
        User seller = userRepository.findById(productDto.getSellerId())
                .orElseThrow(() -> new RuntimeException("Seller not found with id: " + productDto.getSellerId()));

        Product product = productMapper.toEntity(productDto);
        product.setSeller(seller);
        product.setAverageRating(0.0f);
        product.setNumOfReviews(0);

        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    public ProductDto updateProduct(Long id, ProductDto productDto) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (StringUtils.hasText(productDto.getName())) {
            existingProduct.setName(productDto.getName());
        }
        if (StringUtils.hasText(productDto.getDescription())) {
            existingProduct.setDescription(productDto.getDescription());
        }
        if (productDto.getPrice() >= 0) {
            existingProduct.setPrice(productDto.getPrice());
        } else {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (StringUtils.hasText(productDto.getCategory())) {
            existingProduct.setCategory(productDto.getCategory());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        return productMapper.toDto(updatedProduct);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    public ProductDto addImageToProduct(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String filename = "product_" + productId + "_" + (StringUtils.hasText(originalFilename) ? originalFilename.replaceAll("\\s+", "_") : "image.png");
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, REPLACE_EXISTING);

            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/products/image/")
                    .path(filename)
                    .toUriString();

            product.setPhotoUrl(fileUrl);
            Product savedProduct = productRepository.save(product);
            return productMapper.toDto(savedProduct);

        } catch (IOException e) {
            // Log the actual IOException
            log.error("Failed to store file for product id {}: {}", productId, e.getMessage(), e);
            // Wrap in a runtime exception for the controller to handle via GlobalExceptionHandler
            throw new RuntimeException("Failed to store file. " + e.getMessage(), e);
        }
    }
}
