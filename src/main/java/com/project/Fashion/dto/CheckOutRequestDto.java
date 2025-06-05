package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for initiating the checkout process.")
public class CheckOutRequestDto {

    @Schema(description = "ID of the user whose cart is being checked out.", example = "user-uuid-abc", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "User ID cannot be blank.")
    private String userId;

    @Schema(description = "ID of the chosen delivery method for the checkout.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Delivery ID cannot be null.")
    private Long deliveryId;
}