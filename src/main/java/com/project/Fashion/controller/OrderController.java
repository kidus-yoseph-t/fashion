package com.project.Fashion.controller;

import com.project.Fashion.model.Order;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/orders")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;

    //create
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order){
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    //retrieve
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(){
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id){
        return ResponseEntity.ok((orderService.getOrder(id)));
    }

    //update
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order){
        return ResponseEntity.ok(orderService.updateOrder(id, order));
    }

    //patch
    @PatchMapping("/{id}")
    public ResponseEntity<Order> patchOrder(@PathVariable Long id, @RequestBody Map<String, Object> updates){
        return ResponseEntity.ok(orderService.patchOrder(id, updates));
    }

    //delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id){
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
