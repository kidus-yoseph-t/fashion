package com.project.Fashion.service;

import com.project.Fashion.config.mappers.ProductMapper;
import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProductService{

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    private final Path root = Paths.get("uploads");

    public ProductDto addProduct(ProductDto productDto, MultipartFile file) throws IOException {
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        // Save file with unique name
        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        // Map DTO and set photoUrl to access URL path
        Product product = productMapper.toEntity(productDto);
        product.setPhotoUrl("/products/images/" + filename);

        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    public Resource loadImage(String filename) throws IOException {
        Path file = root.resolve(filename);
        if (!Files.exists(file)) {
            throw new IOException("File not found " + filename);
        }
        return new UrlResource(file.toUri());
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }

    public Product updateProduct(Long id, Product newProductData) {
        Product existingProduct = getProduct(id);

        existingProduct.setName(newProductData.getName());
        existingProduct.setDescription(newProductData.getDescription());
        existingProduct.setPrice(newProductData.getPrice());
        existingProduct.setCategory(newProductData.getCategory());
        existingProduct.setPhotoUrl(newProductData.getPhotoUrl());
        existingProduct.setAverageRating(newProductData.getAverageRating());
        existingProduct.setNumOfReviews(newProductData.getNumOfReviews());
        if (newProductData.getSeller() != null && newProductData.getSeller().getId() != null) {
            User seller = userRepository.findById(newProductData.getSeller().getId())
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
            existingProduct.setSeller(seller);
        }

        return productRepository.save(existingProduct);
    }

    @Autowired
    private UserRepository userRepository;

    public Product patchProduct(Long id, Map<String, Object> updates) {
        Product product = getProduct(id);

        updates.forEach((key, value) -> {
            switch (key) {
                case "name" -> product.setName((String) value);
                case "description" -> product.setDescription((String) value);
                case "price" -> product.setPrice(Float.parseFloat(value.toString()));
                case "category" -> product.setCategory((String) value);
                case "photoUrl" -> product.setPhotoUrl((String) value);
                case "sellerId" -> {
                    String sellerId = value.toString();
                    User seller = userRepository.findById(sellerId)
                            .orElseThrow(() -> new IllegalArgumentException("Seller not found with ID: " + sellerId));
                    product.setSeller(seller);
                }
                case "averageRating" -> product.setAverageRating(Float.parseFloat(value.toString()));
                case "numOfReviews" -> product.setNumOfReviews(Integer.parseInt(value.toString()));
                default -> throw new IllegalArgumentException("Invalid field: " + key);
            }
        });

        return productRepository.save(product);
    }


    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);
    }
}

