package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO for API responses containing full product details.")
public class ProductResponseDto {

    @Schema(description = "Unique identifier of the Product.", example = "1")
    private Long id;

    @Schema(description = "Name of the product.", example = "Classic White T-Shirt")
    private String name;

    @Schema(description = "Detailed description of the product.", example = "A comfortable and stylish 100% cotton t-shirt.")
    private String description;

    @Schema(description = "Price of the product.", example = "29.99")
    private Float price;

    @Schema(description = "Category of the product.", example = "Apparel")
    private String category;

    @Schema(description = "URL of the product's image.", example = "http://localhost:8080/api/products/image/product_1_image.png")
    private String photoUrl;

    @Schema(description = "The ID of the seller who owns the product.", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
    private String sellerId;

    @Schema(description = "The full name of the seller.", example = "Alice Seller")
    private String sellerName;

    @Schema(description = "The email of the seller.", example = "alice@sellershop.com")
    private String sellerEmail;

    @Schema(description = "The average rating of the product.", example = "4.5")
    private float averageRating;

    @Schema(description = "The total number of reviews for the product.", example = "150")
    private int numOfReviews;

    @Schema(description = "The total number of products that are available for sale.", example = "12")
    private int stock;
}