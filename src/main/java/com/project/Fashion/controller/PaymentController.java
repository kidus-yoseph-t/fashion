package com.project.Fashion.controller;

import com.project.Fashion.dto.PaymentRequestDto;
import com.project.Fashion.dto.PaymentResponseDto;
import com.project.Fashion.model.Order;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.service.PaymentService;
import com.project.Fashion.exception.exceptions.OrderNotFoundException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Management", description = "APIs for processing order payments.")
@SecurityRequirement(name = "bearerAuth") // All payment operations generally require authentication
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository; // To fetch order for ownership check
    private final UserRepository userRepository;   // To get authenticated user details

    @Autowired
    public PaymentController(PaymentService paymentService,
                             OrderRepository orderRepository,
                             UserRepository userRepository) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName(); // Email
        return userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
    }

    @Operation(summary = "Process a payment for an order",
            description = "Allows an authenticated BUYER (or ADMIN) to process a payment for a specific order. The order must exist and belong to the buyer (unless user is Admin). Payment details are mock/simulated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed (either successfully or with a simulated failure status like 'FAILED')",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payment request data (e.g., missing fields, invalid card details format, amount mismatch)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"mockCardNumber\":\"Card number must be 16 digits\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (e.g., Buyer trying to pay for another user's order, or order not in payable state)"),
            @ApiResponse(responseCode = "404", description = "Order not found for the given orderId")
    })
    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')") // As per SecurityConfig
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        User authenticatedUser = getAuthenticatedUser();

        Order order = orderRepository.findById(paymentRequestDto.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + paymentRequestDto.getOrderId()));

        String userRole = authenticatedUser.getRole().toUpperCase();

        if (!"ADMIN".equals(userRole)) { // If not an admin, check ownership
            if (order.getUser() == null || !order.getUser().getId().equals(authenticatedUser.getId())) {
                throw new AccessDeniedException("User can only process payments for their own orders.");
            }
        }
        // Admins can proceed for any order. Buyers can only proceed for their own if ownership check passes.

        // Additional check for amount matching (also in service, but good for early fail)
        if (Math.abs(order.getTotal() - paymentRequestDto.getAmount()) > 0.01) { // Using a small tolerance
            // Consider if this specific error should be a 400 Bad Request or 403 Forbidden.
            // 400 might be more appropriate if the DTO itself is inconsistent with the target order.
            throw new AccessDeniedException("Payment amount in request does not match order total. Expected: " + order.getTotal() + ", Got: " + paymentRequestDto.getAmount());
        }

        PaymentResponseDto paymentResponse = paymentService.processPayment(paymentRequestDto);
        // The service determines the actual success/failure and includes it in the response.
        // HTTP status is 200 OK regardless of simulated payment success/failure, as the API call itself succeeded.
        return ResponseEntity.ok(paymentResponse);
    }
}
