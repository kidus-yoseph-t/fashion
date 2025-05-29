package com.project.Fashion.service;

import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/products";

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public Page<Product> getAllProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product createProduct(Product product) {
        String sellerId = product.getSeller().getId();
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        product.setSeller(seller);
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public String uploadImage(Long productId, MultipartFile file) {
        Product product = getProduct(productId);
        String photoUrl = photoFunction.apply(productId.toString(), file);
        product.setPhotoUrl(photoUrl);
        productRepository.save(product);
        return photoUrl;
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename)
            .filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1))
            .orElse(".png");

    private final BiFunction<String, MultipartFile, String> photoFunction = (id, image) -> {
        String filename = "product_" + id + fileExtension.apply(image.getOriginalFilename());
        try {
            Path fileStorageLocation = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/products/image/" + filename).toUriString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to save image", e);
        }
    };

    public Product addImageToProduct(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        try {
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate a unique filename
            String filename = "product_" + productId + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, REPLACE_EXISTING);

            // Create a URL to access the image
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("uploads/products/")
                    .path(filename)
                    .toUriString();

            product.setPhotoUrl(fileUrl);
            return productRepository.save(product);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
