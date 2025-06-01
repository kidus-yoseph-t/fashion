package com.project.Fashion.controller;

import com.project.Fashion.dto.CheckOutRequestDto;
import com.project.Fashion.dto.OrderRequestDto;
import com.project.Fashion.dto.OrderResponseDto;
import com.project.Fashion.model.Order;
import com.project.Fashion.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Create a new order
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody OrderRequestDto request) {
        Order order = orderService.createOrderFromDto(request);
        OrderResponseDto responseDto = orderService.convertToDto(order);
        return ResponseEntity.ok(responseDto);
    }




    // Get all orders
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() { // <--- Changed return type
        List<Order> orders = orderService.getAllOrders();
        List<OrderResponseDto> responseDtos = orders.stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }
    // Get a specific order by ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    // Get all orders for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
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
    public ResponseEntity<List<Order>> checkout(@RequestBody CheckOutRequestDto request) {
        return ResponseEntity.ok(orderService.checkout(request.getUserId(), request.getDeliveryId()));
    }

}