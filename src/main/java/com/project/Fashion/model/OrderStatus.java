package com.project.Fashion.model;

public enum OrderStatus {
    PENDING,             // Order created, awaiting further action (e.g., payment)
    PENDING_PAYMENT,     // Order is awaiting payment
    PAID,                // Payment successful, order confirmed
    PROCESSING,          // Order is being processed (e.g., by seller)
    SHIPPED,             // Order has been shipped (if you add shipping)
    COMPLETED,           // Order fulfilled and completed
    CANCELLED,           // Order cancelled by user or system
    PAYMENT_FAILED       // Payment attempt failed
}