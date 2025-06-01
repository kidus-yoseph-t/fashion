// src/main/java/com/project/Fashion/controller/DeliveryController.java
package com.project.Fashion.controller;

import com.project.Fashion.model.Delivery;
import com.project.Fashion.repository.DeliveryRepository; // You'll need this
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/deliveries") // Common practice for plural noun
@AllArgsConstructor // Injects DeliveryRepository if it's a final field
public class DeliveryController {

    private final DeliveryRepository deliveryRepository; // Assuming you create this repository

    @PostMapping
    public ResponseEntity<Delivery> createDelivery(@RequestBody Delivery delivery) {
        Delivery savedDelivery = deliveryRepository.save(delivery);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDelivery);
    }

    @GetMapping
    public ResponseEntity<List<Delivery>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Delivery> getDeliveryById(@PathVariable Long id) {
        Optional<Delivery> delivery = deliveryRepository.findById(id);
        return delivery.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // You might also want PUT/DELETE endpoints for full CRUD
}