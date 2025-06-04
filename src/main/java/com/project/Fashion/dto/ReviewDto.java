package com.project.Fashion.dto;

import lombok.Data;
// No need for AllArgsConstructor if we are setting fields manually or using mappers
// import lombok.AllArgsConstructor;
// import lombok.NoArgsConstructor;

@Data
// @NoArgsConstructor // Lombok's @Data includes @NoArgsConstructor
// @AllArgsConstructor // Remove if you're not using a constructor for all fields
public class ReviewDto {
    private Long id;
    private Long productId;
    private String userId;
    private String userName;
    private float rating;
    private String comment;
    private String date; // ISO string or formatted date
}
