package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO for creating a new product. Contains only fields required from the seller.")
public class ProductCreateDto {

    @Schema(description = "Name of the product.", example = "Classic White T-Shirt", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product name cannot be blank.")
    @Size(min = 3, max = 255)
    private String name;

    @Schema(description = "Detailed description of the product.", example = "A comfortable and stylish 100% cotton t-shirt.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product description cannot be blank.")
    @Size(max = 2000)
    private String description;

    @Schema(description = "Price of the product.", example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product price cannot be null.")
    @DecimalMin(value = "0.01", message = "Product price must be greater than 0.")
    private Float price;

    @Schema(description = "Category of the product.", example = "T-shirt", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product category cannot be blank.")
    @Size(max = 100)
    private String category;

    @Schema(description = "Initial stock quantity of the product.", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Stock quantity cannot be null.")
    @Min(value = 0, message = "Stock quantity cannot be negative.")
    private Integer stock;
}