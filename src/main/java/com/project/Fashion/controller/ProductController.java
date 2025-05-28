package com.project.Fashion.controller;

import com.project.Fashion.config.mappers.ProductMapper;
import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.model.Product;
import com.project.Fashion.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;


import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@AllArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDto> addProduct(
            @RequestPart("product") ProductDto productDto,
            @RequestPart("file") MultipartFile file) throws IOException {

        ProductDto savedProduct = productService.addProduct(productDto, file);
        return ResponseEntity.ok(savedProduct);
    }

    @GetMapping(path = "/images/{filename}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        Resource file = productService.loadImage(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // or determine dynamically
                .body(file);
    }

    // Get All Products
    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductDto> productDtos = products.stream()
                .map(productMapper::toDto)
                .toList();
        return ResponseEntity.ok(productDtos);
    }

    // Get One Product
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        Product product = productService.getProduct(id);
        return ResponseEntity.ok(productMapper.toDto(product));
    }

    // Update Product
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        Product updatedProduct = productService.updateProduct(id, productMapper.toEntity(productDto));
        return ResponseEntity.ok(productMapper.toDto(updatedProduct));
    }

    // Patch Product (partial update)
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> patchProduct(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Product patchedProduct = productService.patchProduct(id, updates);
        return ResponseEntity.ok(productMapper.toDto(patchedProduct));
    }

    // Delete Product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

