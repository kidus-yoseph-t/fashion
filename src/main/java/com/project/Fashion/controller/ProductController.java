package com.project.Fashion.controller;

import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        ProductDto savedProductDto = productService.createProduct(productDto);
        return ResponseEntity.ok(savedProductDto);
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        ProductDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // IOException is no longer declared here as ProductService wraps it.
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) { // Removed "throws IOException"
        ProductDto updatedProductDto = productService.addImageToProduct(id, file);
        return ResponseEntity.ok(updatedProductDto);
    }

    // This method *does* directly call Files.readAllBytes, so it *can* throw IOException.
    @GetMapping(path = "/image/{filename:.+}", produces = {IMAGE_PNG_VALUE, IMAGE_JPEG_VALUE})
    public ResponseEntity<byte[]> getImage(@PathVariable("filename") String filename) throws IOException {
        Path imagePath = Paths.get(PHOTO_DIRECTORY, filename);
        if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageBytes = Files.readAllBytes(imagePath); // This can throw IOException

        // Simplified content type logic
        MediaType contentType;
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG;
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG;
        } else {
            // Consider a default or throw an error for unsupported types
            // For simplicity, defaulting to octet-stream or returning bad request
            // contentType = MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.badRequest().body("Unsupported image type".getBytes());
        }
        return ResponseEntity.ok().contentType(contentType).body(imageBytes);
    }
}
