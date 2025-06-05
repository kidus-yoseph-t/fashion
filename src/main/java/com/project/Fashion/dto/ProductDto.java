package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
@Schema(description = "Data Transfer Object for Product details")
public class ProductDto {

    @Schema(description = "Unique identifier of the Product.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Name of the product.", example = "Classic White T-Shirt", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Detailed description of the product.", example = "A comfortable and stylish 100% cotton t-shirt.")
    private String description;

    @Schema(description = "Price of the product.", example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private float price;

    @Schema(description = "Category of the product.", example = "Apparel", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Schema(description = "URL of the product's image.", example = "http://localhost:8080/api/products/image/product_1_image.png")
    private String photoUrl;

    @Schema(description = "The ID of the seller who owns the product.", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
    private String sellerId;

    @Schema(description = "The average rating of the product based on user reviews.", example = "4.5")
    private float averageRating;

    @Schema(description = "The total number of reviews for the product.", example = "150")
    private int numOfReviews;
}