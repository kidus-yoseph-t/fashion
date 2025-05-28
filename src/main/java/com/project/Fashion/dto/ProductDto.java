package com.project.Fashion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private float price;
    private String category;
    private String photoUrl;
    private String sellerId;
    private float averageRating;
    private int numOfReviews;
}

