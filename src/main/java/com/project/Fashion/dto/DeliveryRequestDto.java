package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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

    @Schema(description = "Minimum estimated delivery days.", example = "5")
    @NotNull(message = "Minimum delivery days cannot be null.")
    @Min(value = 1, message = "Minimum delivery days must be at least 1.")
    private Integer minDeliveryDays;

    @Schema(description = "Maximum estimated delivery days.", example = "7")
    @NotNull(message = "Maximum delivery days cannot be null.")
    @Min(value = 1, message = "Maximum delivery days must be at least 1.")
    private Integer maxDeliveryDays;
}
