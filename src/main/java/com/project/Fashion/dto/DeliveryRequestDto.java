package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for creating or updating a Delivery option.")
public class DeliveryRequestDto {

    @Schema(description = "Type or name of the delivery method (e.g., Standard, Express).", example = "Standard Shipping", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Delivery type cannot be blank.")
    @Size(min = 3, max = 100, message = "Delivery type must be between 3 and 100 characters.")
    private String type;

    @Schema(description = "Cost of this delivery method.", example = "5.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Delivery cost cannot be null.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Delivery cost cannot be negative.") // Allow 0 for free shipping
    private Float deliveryCost; // Changed to Float for @NotNull
}
