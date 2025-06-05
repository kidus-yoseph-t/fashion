package com.project.Fashion.controller;

import com.project.Fashion.dto.DeliveryRequestDto;
import com.project.Fashion.model.Delivery;
import com.project.Fashion.repository.DeliveryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Delivery Management", description = "APIs for managing delivery options.")
@AllArgsConstructor
public class DeliveryController {

    private final DeliveryRepository deliveryRepository;

    @Operation(summary = "Create a new delivery option (Admin only)",
            description = "Allows an administrator to create a new delivery option (e.g., Standard Shipping, Express Shipping).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Delivery option created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Delivery.class))), // Response is still Delivery entity
            @ApiResponse(responseCode = "400", description = "Invalid input data for delivery option",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"type\":\"Delivery type cannot be blank\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Delivery> createDelivery(
            @Parameter(description = "Details of the new delivery option", required = true,
                    content = @Content(schema = @Schema(implementation = DeliveryRequestDto.class)))
            @Valid @RequestBody DeliveryRequestDto deliveryRequestDto) { // Use DTO for request
        Delivery newDelivery = new Delivery();
        newDelivery.setType(deliveryRequestDto.getType());
        newDelivery.setDeliveryCost(deliveryRequestDto.getDeliveryCost());
        // Assuming Delivery entity has no other required fields for creation or they have defaults.
        Delivery savedDelivery = deliveryRepository.save(newDelivery);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDelivery);
    }

    @Operation(summary = "Get all available delivery options (Public)",
            description = "Retrieves a list of all available delivery options.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all delivery options",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Delivery.class))))
    })
    @GetMapping
    public ResponseEntity<List<Delivery>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryRepository.findAll());
    }

    @Operation(summary = "Get a specific delivery option by ID (Public)",
            description = "Retrieves details for a specific delivery option by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved delivery option details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Delivery.class))),
            @ApiResponse(responseCode = "404", description = "Delivery option not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Delivery> getDeliveryById(
            @Parameter(description = "ID of the delivery option to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        Optional<Delivery> delivery = deliveryRepository.findById(id);
        return delivery.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update an existing delivery option (Admin only)",
            description = "Allows an administrator to update an existing delivery option.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Delivery option updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Delivery.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Delivery option not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Delivery> updateDelivery(
            @Parameter(description = "ID of the delivery option to update", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Updated details for the delivery option", required = true,
                    content = @Content(schema = @Schema(implementation = DeliveryRequestDto.class)))
            @Valid @RequestBody DeliveryRequestDto deliveryRequestDto) { // Use DTO for request
        return deliveryRepository.findById(id)
                .map(existingDelivery -> {
                    existingDelivery.setType(deliveryRequestDto.getType());
                    existingDelivery.setDeliveryCost(deliveryRequestDto.getDeliveryCost());
                    Delivery updatedDelivery = deliveryRepository.save(existingDelivery);
                    return ResponseEntity.ok(updatedDelivery);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a delivery option (Admin only)",
            description = "Allows an administrator to delete a delivery option.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Delivery option deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Delivery option not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDelivery(
            @Parameter(description = "ID of the delivery option to delete", required = true, example = "1") @PathVariable Long id) {
        if (!deliveryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        deliveryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
