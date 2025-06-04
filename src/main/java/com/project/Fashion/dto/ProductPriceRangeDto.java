package com.project.Fashion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceRangeDto {
    private float minPrice;
    private float maxPrice;
}
