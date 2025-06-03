package com.project.Fashion.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private String transactionId; // A mock transaction ID (e.g., UUID)
    private String status; // e.g., "SUCCESS", "FAILED"
    private String message; // e.g., "Payment processed successfully", "Payment failed: Insufficient funds (mock)"
    private Long orderId;
}