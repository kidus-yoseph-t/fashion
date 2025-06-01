package com.project.Fashion.controller;

import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Static imports for media types are fine
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private static final String PHOTO_DIRECTORY = "src/main/resources/static/uploads/products/";

    // Public endpoint, no specific role needed beyond what's in SecurityConfig (permitAll for GET)
    // However, if creation is restricted to SELLERs, SecurityConfig handles that.
    // Let's assume SecurityConfig's .requestMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER") is the primary guard.
    // If an additional check at the method level is desired, @PreAuthorize("hasRole('SELLER')") could be added.
    // For now, relying on SecurityConfig for POST /api/products
    @PostMapping
    @PreAuthorize("hasRole('SELLER')") // Adding explicit check here for clarity, aligns with SecurityConfig
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        ProductDto savedProductDto = productService.createProduct(productDto);
        return ResponseEntity.ok(savedProductDto);
    }

    // Public endpoint (permitAll for GET in SecurityConfig)
    @GetMapping
    public ResponseEntity<Page<ProductDto>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Float minPrice,
            @RequestParam(required = false) Float maxPrice,
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir) {
        Page<ProductDto> productPage = productService.getAllProducts(page, size, category, searchTerm, minPrice, maxPrice, minRating, sortBy, sortDir);
        return ResponseEntity.ok(productPage);
    }

    // Public endpoint (permitAll for GET in SecurityConfig)
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    // Seller can update their own product. Admin can update any product (if that's the desired logic).
    // Based on SecurityConfig: .requestMatchers(HttpMethod.PUT, "/api/products/{id:[0-9]+}").hasRole("SELLER") // Seller can update (their own - checked via @PreAuthorize)
    // So, only seller, and they must be the owner.
    @PreAuthorize("hasRole('SELLER') and @productSecurity.isOwner(authentication, #id)")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        ProductDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    // Seller can delete their own product, Admin can delete any product.
    // SecurityConfig: .requestMatchers(HttpMethod.DELETE, "/api/products/{id:[0-9]+}").hasAnyRole("SELLER", "ADMIN")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and @productSecurity.isOwner(authentication, #id))")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Only the seller who owns the product can upload an image for it.
    // SecurityConfig: .requestMatchers(HttpMethod.POST, "/api/products/{id:[0-9]+}/image").hasRole("SELLER")
    @PreAuthorize("hasRole('SELLER') and @productSecurity.isOwner(authentication, #id)")
    public ResponseEntity<ProductDto> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        ProductDto updatedProductDto = productService.addImageToProduct(id, file);
        return ResponseEntity.ok(updatedProductDto);
    }

    // Public endpoint (permitAll for GET /api/products/image/** in SecurityConfig)
    @GetMapping(path = "/image/{filename:.+}", produces = {IMAGE_PNG_VALUE, IMAGE_JPEG_VALUE})
    public ResponseEntity<byte[]> getImage(@PathVariable("filename") String filename) throws IOException {
        Path imagePath = Paths.get(PHOTO_DIRECTORY, filename);
        if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageBytes = Files.readAllBytes(imagePath);

        MediaType contentType;
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG;
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG;
        } else {
            return ResponseEntity.badRequest().body("Unsupported image type".getBytes());
        }
        return ResponseEntity.ok().contentType(contentType).body(imageBytes);
    }
}
