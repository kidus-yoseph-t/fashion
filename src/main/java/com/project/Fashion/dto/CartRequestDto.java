package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for adding an item to the shopping cart.")
public class CartRequestDto {

    @Schema(description = "ID of the user adding the item to the cart. Must match authenticated user.", example = "user-uuid-abc", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "User ID cannot be blank.")
    private String userId;

    @Schema(description = "ID of the product to add to the cart.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product ID cannot be null.")
    private Long productId;

    @Schema(description = "Quantity of the product to add.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity cannot be null.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity; // Changed to Integer for @NotNull
}
