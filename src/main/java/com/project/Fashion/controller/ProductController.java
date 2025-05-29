package com.project.Fashion.controller;

import com.project.Fashion.model.Product;
import com.project.Fashion.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.http.MediaType.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    private static final String PHOTO_DIRECTORY = System.getProperty("user.dir") + "/uploads/";

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.createProduct(product);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getAllProducts(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Product updatedProduct = productService.addImageToProduct(id, file);
        return ResponseEntity.ok(updatedProduct);
    }


    @GetMapping(path = "/image/{filename}", produces = {IMAGE_PNG_VALUE, IMAGE_JPEG_VALUE})
    public byte[] getImage(@PathVariable("filename") String filename) throws IOException {
        return Files.readAllBytes(Paths.get(PHOTO_DIRECTORY + filename));
    }
}
