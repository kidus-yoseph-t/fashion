package com.project.Fashion.controller;

import com.project.Fashion.model.Order;
import com.project.Fashion.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Create a new order
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    // Get all orders
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // Get a specific order by ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    // Update an order fully
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return ResponseEntity.ok(orderService.updateOrder(id, order));
    }

    // Patch (partial update) an order
    @PatchMapping("/{id}")
    public ResponseEntity<Order> patchOrder(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(orderService.patchOrder(id, updates));
    }

    // Delete an order
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // Checkout: move cart items to orders
    @PostMapping("/checkout")
    public ResponseEntity<List<Order>> checkout(@RequestParam String userId, @RequestParam Long deliveryId) {
        return ResponseEntity.ok(orderService.checkout(userId, deliveryId));
    }
}
