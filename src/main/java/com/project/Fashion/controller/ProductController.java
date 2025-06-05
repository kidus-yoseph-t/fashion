package com.project.Fashion.controller;

import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.dto.ProductPriceRangeDto;
import com.project.Fashion.service.ProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for browsing, creating, and managing products.")
public class ProductController {

    private final ProductService productService;
    private static final String PHOTO_DIRECTORY = "src/main/resources/static/uploads/products/";

    @Operation(summary = "Create a new product (Seller only)",
            description = "Allows an authenticated SELLER to create a new product listing. The seller ID from the DTO will be overridden by the authenticated seller's ID.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product data provided"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a SELLER)")
    })
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto savedProductDto = productService.createProduct(productDto);
        return ResponseEntity.ok(savedProductDto);
    }

    @Operation(summary = "Get a paginated list of products (Public)",
            description = "Retrieves a list of products with filtering, sorting, and pagination options.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<Page<ProductDto>> getProducts(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by category (case-insensitive)", example = "Apparel") @RequestParam(required = false) String category,
            @Parameter(description = "Search term for name or description", example = "shirt") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Minimum price", example = "10.99") @RequestParam(required = false) Float minPrice,
            @Parameter(description = "Maximum price", example = "99.50") @RequestParam(required = false) Float maxPrice,
            @Parameter(description = "Minimum average rating (1-5)", example = "3.5") @RequestParam(required = false) Float minRating,
            @Parameter(description = "Field to sort by (name, price, averageRating, id)", example = "price") @RequestParam(required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC") @RequestParam(required = false, defaultValue = "ASC") String sortDir) {
        Page<ProductDto> productPage = productService.getAllProducts(page, size, category, searchTerm, minPrice, maxPrice, minRating, sortBy, sortDir);
        return ResponseEntity.ok(productPage);
    }

    @Operation(summary = "Get a specific product by ID (Public)",
            description = "Retrieves details for a specific product by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved product details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @GetMapping("/{id}")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<ProductDto> getProduct(@Parameter(description = "ID of the product to retrieve", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Update an existing product (Seller only, Owner only)",
            description = "Allows an authenticated SELLER to update a product they own.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not SELLER or not owner)"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') and @productSecurity.isOwner(authentication, #id)")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<ProductDto> updateProduct(
            @Parameter(description = "ID of the product to update", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProductDto productDto) {
        ProductDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Delete a product (Admin or Seller Owner only)",
            description = "Deletes a product. Requires ADMIN privileges, or SELLER must own the product.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and @productSecurity.isOwner(authentication, #id))")
    public ResponseEntity<Void> deleteProduct(@Parameter(description = "ID of the product to delete", example = "1") @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload an image for a product (Seller only, Owner only)",
            description = "Allows an authenticated SELLER to upload/replace an image for a product they own.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image uploaded and product updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "No file provided or invalid file"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not SELLER or not owner)"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Failed to store file"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER') and @productSecurity.isOwner(authentication, #id)")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<ProductDto> uploadImage(
            @Parameter(description = "ID of the product for image upload", example = "1") @PathVariable Long id,
            @Parameter(description = "Image file (PNG, JPG/JPEG)", required = true) @RequestParam("file") MultipartFile file) {
        ProductDto updatedProductDto = productService.addImageToProduct(id, file);
        return ResponseEntity.ok(updatedProductDto);
    }

    @Operation(summary = "Get a product image (Public)",
            description = "Retrieves an image file by its filename.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image retrieved successfully",
                    content = {@Content(mediaType = IMAGE_PNG_VALUE), @Content(mediaType = IMAGE_JPEG_VALUE)}),
            @ApiResponse(responseCode = "400", description = "Unsupported image type"),
            @ApiResponse(responseCode = "404", description = "Image not found")
    })
    @GetMapping(path = "/image/{filename:.+}", produces = {IMAGE_PNG_VALUE, IMAGE_JPEG_VALUE})
    public ResponseEntity<byte[]> getImage(@Parameter(description = "Filename of the image", example = "product_1_image.png") @PathVariable("filename") String filename) throws IOException {
        Path imagePath = Paths.get(PHOTO_DIRECTORY, filename);
        if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageBytes = Files.readAllBytes(imagePath);
        MediaType contentType;
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) contentType = MediaType.IMAGE_PNG;
        else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) contentType = MediaType.IMAGE_JPEG;
        else return ResponseEntity.badRequest().body("Unsupported image type".getBytes());
        return ResponseEntity.ok().contentType(contentType).body(imageBytes);
    }

    @Operation(summary = "Get products listed by the authenticated seller (Seller only)",
            description = "Retrieves a paginated list of products listed by the currently authenticated SELLER. Supports sorting.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved seller's products",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Page.class))), // Schema should represent Page<ProductDto>
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a SELLER)")
    })
    @GetMapping("/seller/me")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<ProductDto>> getMyProducts(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., name, price, averageRating, id). Default: name.", example = "name") @RequestParam(required = false, defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC). Default: ASC.", example = "ASC") @RequestParam(required = false, defaultValue = "ASC") String sortDir
    ) {
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDir.equalsIgnoreCase("DESC")) {
            direction = Sort.Direction.DESC;
        }
        // Validate sortBy field to prevent invalid sort properties
        List<String> validSortProperties = List.of("name", "price", "averageRating", "id");
        String sortProperty = validSortProperties.contains(sortBy) ? sortBy : "name";

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
        Page<ProductDto> products = productService.getProductsByAuthenticatedSeller(pageable);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get distinct product categories (Public)",
            description = "Retrieves a list of all unique product category names.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved distinct categories",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(type = "string", example = "Apparel"))))
    })
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getDistinctCategories() {
        List<String> categories = productService.getDistinctCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Get product price range metadata (Public)",
            description = "Retrieves the overall minimum and maximum prices of products.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved product price range",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductPriceRangeDto.class)))
    })
    @GetMapping("/price-range-meta")
    public ResponseEntity<ProductPriceRangeDto> getProductPriceRange() {
        ProductPriceRangeDto priceRange = productService.getProductPriceRange();
        return ResponseEntity.ok(priceRange);
    }
}
