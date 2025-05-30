package com.project.Fashion.dto;

import lombok.Data;

import java.util.Date;

@Data
public class OrderRequestDto {
    private String user;     // user ID as string
    private Long product;    // product ID
    private Date date;
    private int quantity;
    private float total;
    private Long delivery;   // delivery ID
}
