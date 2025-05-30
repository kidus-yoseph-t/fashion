package com.project.Fashion.dto;



import lombok.Data;

@Data
public class ReviewDto {
    private Long id;
    private Long productId;
    private String userId;
    private float rating;
    private String comment;
    private String date; // ISO string or formatted date
}
