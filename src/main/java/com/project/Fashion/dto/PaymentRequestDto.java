package com.project.Fashion.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    @NotNull(message = "Amount cannot be null") // Added NotNull for the primitive wrapper if it were Double
    @Positive(message = "Amount must be positive")
    private double amount; // double is a primitive, @NotNull is for objects. @Positive handles > 0.

    // Mock card details - not validated for real security, just for simulation
    @NotBlank(message = "Card number cannot be blank")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be 16 digits")
    private String mockCardNumber;

    @NotBlank(message = "Expiry date cannot be blank")
    @Pattern(regexp = "^(0[1-9]|1[0-2])\\/([0-9]{2})$", message = "Expiry date must be in MM/YY format")
    private String mockExpiryDate; // e.g., "12/25"

    @NotBlank(message = "CVV cannot be blank")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
    private String mockCvv;
}