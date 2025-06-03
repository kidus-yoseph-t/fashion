package com.project.Fashion.dto;

import jakarta.validation.constraints.Min; // For validation
import lombok.Data;

@Data
public class CartItemQuantityUpdateDto {
    @Min(value = 1, message = "Quantity must be at least 1.") // Ensures positive quantity
    private int quantity;
}