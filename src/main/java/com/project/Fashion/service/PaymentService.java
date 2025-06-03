package com.project.Fashion.service;

import com.project.Fashion.dto.PaymentRequestDto;
import com.project.Fashion.dto.PaymentResponseDto;
import com.project.Fashion.model.Order;
import com.project.Fashion.model.OrderStatus;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.exception.exceptions.OrderNotFoundException;
import com.project.Fashion.exception.exceptions.InvalidFieldException; // For payment processing issues

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService; // To update order status

    @Autowired
    public PaymentService(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequestDto) {
        logger.info("Processing payment for order ID: {}", paymentRequestDto.getOrderId());

        Order order = orderRepository.findById(paymentRequestDto.getOrderId())
                .orElseThrow(() -> {
                    logger.warn("Order not found for payment processing: {}", paymentRequestDto.getOrderId());
                    return new OrderNotFoundException("Order not found with ID: " + paymentRequestDto.getOrderId());
                });

        // Validate order status (e.g., must be PENDING_PAYMENT or PENDING)
        if (!(order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.PENDING)) {
            logger.warn("Order {} is not in a payable state. Current status: {}", order.getId(), order.getStatus());
            throw new InvalidFieldException("Order is not in a payable state. Current status: " + order.getStatus());
        }

        // Validate amount (optional, but good practice)
        // Using a small tolerance for float comparison
        if (Math.abs(order.getTotal() - paymentRequestDto.getAmount()) > 0.01) {
            logger.warn("Payment amount {} does not match order total {} for order ID: {}",
                    paymentRequestDto.getAmount(), order.getTotal(), order.getId());
            // For a mock, we might proceed or fail. Let's choose to fail for this example.
            updateOrderStatus(order, OrderStatus.PAYMENT_FAILED, "Amount mismatch.");
            return new PaymentResponseDto(
                    UUID.randomUUID().toString(),
                    "FAILED",
                    "Payment amount does not match order total.",
                    order.getId()
            );
        }

        // Simulate payment processing based on mock card number
        String mockCardNumber = paymentRequestDto.getMockCardNumber();
        PaymentResponseDto paymentResponse;

        // Simulate a short delay for "processing"
        try {
            Thread.sleep(1000 + new Random().nextInt(2000)); // Simulate 1-3 seconds delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Payment processing delay interrupted", e);
            // Handle interruption, perhaps by failing the payment
            updateOrderStatus(order, OrderStatus.PAYMENT_FAILED, "Processing interrupted.");
            return new PaymentResponseDto(UUID.randomUUID().toString(), "FAILED", "Payment processing interrupted.", order.getId());
        }


        if (mockCardNumber.endsWith("0000")) {
            // Simulate successful payment
            logger.info("Payment successful for order ID: {}", order.getId());
            updateOrderStatus(order, OrderStatus.PAID, "Payment successful.");
            paymentResponse = new PaymentResponseDto(
                    UUID.randomUUID().toString(),
                    "SUCCESS",
                    "Payment processed successfully.",
                    order.getId()
            );
        } else if (mockCardNumber.endsWith("1111")) {
            // Simulate insufficient funds
            logger.warn("Payment failed (Insufficient Funds) for order ID: {}", order.getId());
            updateOrderStatus(order, OrderStatus.PAYMENT_FAILED, "Insufficient funds.");
            paymentResponse = new PaymentResponseDto(
                    UUID.randomUUID().toString(),
                    "FAILED",
                    "Payment failed: Insufficient funds.",
                    order.getId()
            );
        } else if (mockCardNumber.endsWith("2222")) {
            // Simulate card declined
            logger.warn("Payment failed (Card Declined) for order ID: {}", order.getId());
            updateOrderStatus(order, OrderStatus.PAYMENT_FAILED, "Card declined.");
            paymentResponse = new PaymentResponseDto(
                    UUID.randomUUID().toString(),
                    "FAILED",
                    "Payment failed: Card declined.",
                    order.getId()
            );
        } else {
            // Simulate random success/failure for other cards
            Random random = new Random();
            if (random.nextInt(10) < 7) { // 70% chance of success
                logger.info("Mock payment successful (random) for order ID: {}", order.getId());
                updateOrderStatus(order, OrderStatus.PAID, "Payment successful (random).");
                paymentResponse = new PaymentResponseDto(
                        UUID.randomUUID().toString(),
                        "SUCCESS",
                        "Payment processed successfully (random).",
                        order.getId()
                );
            } else { // 30% chance of generic failure
                logger.warn("Mock payment failed (Generic Error) for order ID: {}", order.getId());
                updateOrderStatus(order, OrderStatus.PAYMENT_FAILED, "Generic payment error.");
                paymentResponse = new PaymentResponseDto(
                        UUID.randomUUID().toString(),
                        "FAILED",
                        "Payment failed: Generic payment error.",
                        order.getId()
                );
            }
        }
        return paymentResponse;
    }

    private void updateOrderStatus(Order order, OrderStatus newStatus, String reason) {
        try {
            // Use OrderService to update the status.
            // The patchOrder method in OrderService should be able to handle updating the status.
            // It expects a Map of updates.
            Map<String, Object> updates = Map.of("status", newStatus.name());
            orderService.patchOrder(order.getId(), updates);
            logger.info("Order ID: {} status updated to {} due to: {}", order.getId(), newStatus, reason);
        } catch (Exception e) {
            // Log if updating order status fails, but the payment response is already determined.
            logger.error("Failed to update order status for order ID: {} to {} after payment processing. Reason: {}",
                    order.getId(), newStatus, e.getMessage(), e);
            // This failure to update status is an internal issue and shouldn't change the payment outcome already decided.
        }
    }
}
