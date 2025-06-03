package com.project.Fashion.controller;

import com.project.Fashion.dto.PaymentRequestDto;
import com.project.Fashion.dto.PaymentResponseDto;
import com.project.Fashion.model.Order;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.OrderRepository; // To verify order ownership
import com.project.Fashion.repository.UserRepository;  // To get authenticated user
import com.project.Fashion.service.PaymentService;
import com.project.Fashion.exception.exceptions.OrderNotFoundException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
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

    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        User authenticatedUser = getAuthenticatedUser();

        // Fetch the order to verify ownership before processing payment
        Order order = orderRepository.findById(paymentRequestDto.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + paymentRequestDto.getOrderId()));

        // Authorization Check:
        // Ensure the authenticated user is the one who placed the order, or an Admin.
        // Role check for BUYER to access this endpoint should be in SecurityConfig.
        String userRole = authenticatedUser.getRole().toUpperCase();

        if (!"ADMIN".equals(userRole)) { // If not an admin, check ownership
            if (order.getUser() == null || !order.getUser().getId().equals(authenticatedUser.getId())) {
                throw new AccessDeniedException("User can only process payments for their own orders.");
            }
        }
        // Admins can proceed for any order. Buyers can only proceed for their own.

        // Additional check: Ensure the amount in the request matches the order total
        // This is also done in PaymentService, but an early check here can be good.
        if (Math.abs(order.getTotal() - paymentRequestDto.getAmount()) > 0.01) {
            throw new AccessDeniedException("Payment amount in request does not match order total.");
        }


        PaymentResponseDto paymentResponse = paymentService.processPayment(paymentRequestDto);
        return ResponseEntity.ok(paymentResponse);
    }
}
