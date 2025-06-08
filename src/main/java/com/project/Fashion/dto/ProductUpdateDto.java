package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Fields are not marked with @NotBlank or @NotNull to allow for partial updates (PATCH).
// For PUT, you would add @NotBlank and @NotNull.

@Data
@Schema(description = "DTO for updating an existing product. All fields are optional for PATCH, but required for PUT.")
public class ProductUpdateDto {

    @Schema(description = "New name of the product.", example = "Premium White T-Shirt")
    @Size(min = 3, max = 255)
    private String name;

    @Schema(description = "New detailed description of the product.", example = "A premium, comfortable, and stylish 100% Pima cotton t-shirt.")
    @Size(max = 2000)
    private String description;

    @Schema(description = "New price of the product.", example = "39.99")
    @DecimalMin(value = "0.01", message = "Product price must be greater than 0.")
    private Float price;

    @Schema(description = "New category of the product.", example = "Tops")
    @Size(max = 100)
    private String category;

    @Schema(description = "New stock quantity for the product.", example = "150")
    @Min(value = 0, message = "Stock quantity cannot be negative.")
    private Integer stock;
}