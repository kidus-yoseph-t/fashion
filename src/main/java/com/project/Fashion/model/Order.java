package com.project.Fashion.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user_id", columnList = "user_id"),
        @Index(name = "idx_order_product_id", columnList = "product_id"),
        @Index(name = "idx_order_delivery_id", columnList = "delivery_id"),
        @Index(name = "idx_order_date", columnList = "date"),
        @Index(name = "idx_order_status", columnList = "status")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-orders") // Use a unique name if multiple back refs of the same type exist
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference("product-orders")
    private Product product;

    @Column(nullable = false)
    private Date date;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private float total;

    @Enumerated(EnumType.STRING) // Store status as a string in the DB
    @Column(nullable = false)
    private OrderStatus status; // You'll need an OrderStatus enum

    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    @JsonBackReference("delivery-orders")
    private Delivery delivery;
}