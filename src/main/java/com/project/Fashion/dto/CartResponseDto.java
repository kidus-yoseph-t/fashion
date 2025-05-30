package com.project.Fashion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResponseDto {
    private Long id;
    private int quantity;

    // Product details
    private Long productId;
    private String productName;
    private String photoUrl;
    private String category;
    private double price;

    private String userId;
}

