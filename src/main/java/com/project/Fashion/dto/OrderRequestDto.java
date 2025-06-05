package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema; // Added for consistency
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "Data Transfer Object for creating or updating an Order.")
public class OrderRequestDto {

    @Schema(description = "ID of the user placing the order.", example = "user-uuid-abc", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "User ID cannot be blank.") // User ID is String (UUID)
    private String user;

    @Schema(description = "ID of the product being ordered.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product ID cannot be null.")
    private Long product;

    @Schema(description = "Date of the order. If not provided, defaults to current date. For updates, can be provided.", example = "2024-06-05T14:30:00.000Z")
    @NotNull(message = "Order date cannot be null.") // If it's always set by backend, this might not be needed on input DTO.
    // If client can set it, then NotNull is good.
    // If it's for future orders only: @FutureOrPresent
    private Date date;

    @Schema(description = "Quantity of the product being ordered.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity cannot be null.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity; // Changed to Integer for @NotNull

    @Schema(description = "Total price for this order item.", example = "59.98", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Total price cannot be null.")
    @Min(value = 0, message = "Total price cannot be negative.") // Allow 0 for free items, or use DecimalMin(value="0.0", inclusive=false)
    private Float total; // Changed to Float for @NotNull

    @Schema(description = "ID of the delivery option chosen for the order.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Delivery ID cannot be null.")
    private Long delivery;
}