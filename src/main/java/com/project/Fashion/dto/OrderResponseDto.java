package com.project.Fashion.dto;// com.project.Fashion.dto.OrderResponseDto
import lombok.Data;
import java.util.Date;

@Data
public class OrderResponseDto {
    private Long id;
    private String userId;
    private Long productId;
    private String productName;
    private float productPrice;
    private int quantity;
    private float total;
    private Date date;
    private Long deliveryId;
    private String deliveryMethod;
    private String status; // Add status field
}