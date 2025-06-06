package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
// NoArgsConstructor can be useful if an empty constructor is needed by some frameworks,
// but @Data and @AllArgsConstructor might cover most use cases.
// import lombok.NoArgsConstructor;

@AllArgsConstructor
// @NoArgsConstructor
@Data
@Schema(description = "Data Transfer Object for Product details. Used for creating and updating products.")
public class ProductDto {

    @Schema(description = "Unique identifier of the Product. Not required for creation, but present for responses/updates.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id; // ID is typically not part of create request body from client

    @Schema(description = "Name of the product.", example = "Classic White T-Shirt", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product name cannot be blank.")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters.")
    private String name;

    @Schema(description = "Detailed description of the product.", example = "A comfortable and stylish 100% cotton t-shirt.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product description cannot be blank.")
    @Size(max = 2000, message = "Product description cannot exceed 2000 characters.")
    private String description;

    @Schema(description = "Price of the product.", example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product price cannot be null.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Product price must be greater than 0.")
    private Float price; // Changed to Float to allow @NotNull. Primitive float cannot be null.

    @Schema(description = "Category of the product.", example = "Apparel", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product category cannot be blank.")
    @Size(max = 100, message = "Product category cannot exceed 100 characters.")
    private String category;

    @Schema(description = "URL of the product's image. Optional for creation/update, will be set by image upload endpoint.", example = "http://localhost:8080/api/products/image/product_1_image.png", accessMode = Schema.AccessMode.READ_ONLY)
    private String photoUrl; // Typically set via a separate image upload endpoint, not directly in DTO for create/update main details

    @Schema(description = "The ID of the seller who owns the product. Set by the system based on authenticated user for new products.", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8", accessMode = Schema.AccessMode.READ_ONLY)
    private String sellerId; // Set by backend based on authenticated SELLER

    @Schema(description = "The average rating of the product based on user reviews.", example = "4.5", accessMode = Schema.AccessMode.READ_ONLY)
    private float averageRating; // Calculated by backend

    @Schema(description = "The total number of reviews for the product.", example = "150", accessMode = Schema.AccessMode.READ_ONLY)
    private int numOfReviews; // Calculated by backend
}