package com.project.Fashion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Product name cannot be blank.")
    @Size(min = 3, message = "Product name must be at least 3 characters.")
    private String name;

    @NotBlank(message = "Description cannot be blank.")
    private String description;

    @NotNull(message = "Price cannot be null.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive.")
    private Float price;

    @NotBlank(message = "Category cannot be blank.")
    private String category;

    @NotNull(message = "Stock quantity cannot be null.")
    @Min(value = 0, message = "Stock cannot be negative.")
    private Integer stock;
}