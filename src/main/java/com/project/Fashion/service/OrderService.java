package com.project.Fashion.service;

import com.project.Fashion.dto.OrderRequestDto;
import com.project.Fashion.dto.OrderResponseDto;
import com.project.Fashion.exception.exceptions.*;
import com.project.Fashion.model.Cart;
import com.project.Fashion.model.Delivery;
import com.project.Fashion.model.Order;
import com.project.Fashion.model.OrderStatus; // Import OrderStatus
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.CartRepository;
import com.project.Fashion.repository.DeliveryRepository;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Ensure transactional for updates

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Transactional // Good to have for service methods modifying data
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final DeliveryRepository deliveryRepository;

    // Create a single order
    public Order createOrder(Order order) {
        if (order.getProduct() != null && order.getProduct().getId() != null) {
            Product product = productRepository.findById(order.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + order.getProduct().getId()));
            order.setProduct(product);
        }

        if (order.getUser() != null && order.getUser().getId() != null) {
            User user = userRepository.findById(order.getUser().getId())
                    .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + order.getUser().getId()));
            order.setUser(user);
        }
        // Set initial status if not already set
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING_PAYMENT); // Or PENDING, depending on flow
        }

        return orderRepository.save(order);
    }

    public Order createOrderFromDto(OrderRequestDto dto) {
        User user = userRepository.findById(dto.getUser())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + dto.getUser()));

        Product product = productRepository.findById(dto.getProduct())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + dto.getProduct()));

        Delivery delivery = deliveryRepository.findById(dto.getDelivery())
                .orElseThrow(() -> new DeliveryNotFoundException("Delivery not found with ID: " + dto.getDelivery()));

        Order order = new Order();
        order.setUser(user);
        order.setProduct(product);
        order.setDate(dto.getDate() != null ? dto.getDate() : new Date()); // Default to now if date is null
        order.setQuantity(dto.getQuantity());
        order.setTotal(dto.getTotal());
        order.setDelivery(delivery);
        order.setStatus(OrderStatus.PENDING_PAYMENT); // Initial status for new orders from DTO

        return orderRepository.save(order);
    }

    // Get all orders
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Get a specific order
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
    }

    public List<Order> getOrdersByUserId(String userId) {
        // Optional: Check if user exists, though controller might do this
        // userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return orderRepository.findByUserId(userId);
    }


    // Update a full order
    public Order updateOrder(Long id, Order updatedOrderDetails) {
        Order existingOrder = getOrder(id);

        // Carefully map fields from updatedOrderDetails to existingOrder
        // Ensure not to overwrite critical immutable fields or user unintentionally
        if (updatedOrderDetails.getUser() != null && updatedOrderDetails.getUser().getId() != null) {
            User user = userRepository.findById(updatedOrderDetails.getUser().getId())
                    .orElseThrow(() -> new UserNotFoundException("User not found for order update: " + updatedOrderDetails.getUser().getId()));
            existingOrder.setUser(user);
        }
        if (updatedOrderDetails.getProduct() != null && updatedOrderDetails.getProduct().getId() != null) {
            Product product = productRepository.findById(updatedOrderDetails.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found for order update: " + updatedOrderDetails.getProduct().getId()));
            existingOrder.setProduct(product);
        }
        if (updatedOrderDetails.getDelivery() != null && updatedOrderDetails.getDelivery().getId() != null) {
            Delivery delivery = deliveryRepository.findById(updatedOrderDetails.getDelivery().getId())
                    .orElseThrow(() -> new DeliveryNotFoundException("Delivery not found for order update: " + updatedOrderDetails.getDelivery().getId()));
            existingOrder.setDelivery(delivery);
        }

        existingOrder.setQuantity(updatedOrderDetails.getQuantity());
        existingOrder.setDate(updatedOrderDetails.getDate() != null ? updatedOrderDetails.getDate() : existingOrder.getDate());
        existingOrder.setTotal(updatedOrderDetails.getTotal());
        if (updatedOrderDetails.getStatus() != null) {
            existingOrder.setStatus(updatedOrderDetails.getStatus());
        }
        // Add other updatable fields as necessary

        return orderRepository.save(existingOrder);
    }

    // Patch specific fields
    public Order patchOrder(Long id, Map<String, Object> updates) {
        Order order = getOrder(id);

        updates.forEach((key, value) -> {
            switch (key) {
                case "quantity" -> {
                    if (value == null) throw new InvalidFieldException("Quantity cannot be null.");
                    order.setQuantity(Integer.parseInt(value.toString()));
                }
                case "total" -> {
                    if (value == null) throw new InvalidFieldException("Total cannot be null.");
                    order.setTotal(Float.parseFloat(value.toString()));
                }
                case "date" -> {
                    if (value == null) throw new InvalidFieldException("Date cannot be null.");
                    // Consider more robust date parsing if different formats are possible
                    order.setDate(java.sql.Date.valueOf(value.toString()));
                }
                case "userId" -> {
                    if (value == null) throw new InvalidFieldException("User ID cannot be null.");
                    User user = userRepository.findById(value.toString())
                            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + value.toString()));
                    order.setUser(user);
                }
                case "productId" -> {
                    if (value == null) throw new InvalidFieldException("Product ID cannot be null.");
                    Product product = productRepository.findById(Long.parseLong(value.toString()))
                            .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + value.toString()));
                    order.setProduct(product);
                }
                case "deliveryId" -> {
                    if (value == null) throw new InvalidFieldException("Delivery ID cannot be null.");
                    Delivery delivery = deliveryRepository.findById(Long.parseLong(value.toString()))
                            .orElseThrow(() -> new DeliveryNotFoundException("Delivery not found with ID: " + value.toString()));
                    order.setDelivery(delivery);
                }
                case "status" -> { // <<< --- ADDED CASE FOR STATUS
                    if (value == null) throw new InvalidFieldException("Status cannot be null.");
                    try {
                        order.setStatus(OrderStatus.valueOf(value.toString().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new InvalidFieldException("Invalid status value: " + value.toString());
                    }
                }
                default -> throw new InvalidFieldException("Invalid field for order patch: " + key);
            }
        });

        return orderRepository.save(order);
    }

    // Delete an order
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Order not found with ID: " + id);
        }
        orderRepository.deleteById(id);
    }

    // Checkout: Move cart items to orders for a user
    public List<Order> checkout(String userId, Long deliveryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException("Delivery option not found with ID: " + deliveryId));

        List<Cart> cartItems = cartRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new CartEmptyException("Cart is empty for user ID: " + userId);
        }

        List<Order> orders = new ArrayList<>();
        for (Cart cart : cartItems) {
            Order order = new Order();
            order.setUser(user);
            order.setProduct(cart.getProduct());
            order.setQuantity(cart.getQuantity());
            order.setDate(new Date()); // Order date is now
            order.setTotal(cart.getProduct().getPrice() * cart.getQuantity());
            order.setDelivery(delivery);
            order.setStatus(OrderStatus.PENDING_PAYMENT); // Initial status after checkout
            orders.add(orderRepository.save(order));
        }

        cartRepository.deleteAll(cartItems); // Clear the cart after creating orders
        return orders;
    }

    public OrderResponseDto convertToDto(Order order) {
        if (order == null) return null;
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
        }
        if (order.getProduct() != null) {
            dto.setProductId(order.getProduct().getId());
            dto.setProductName(order.getProduct().getName());
            dto.setProductPrice(order.getProduct().getPrice()); // Added product price to DTO
        }
        dto.setDate(order.getDate());
        dto.setQuantity(order.getQuantity());
        dto.setTotal(order.getTotal());
        if (order.getDelivery() != null) {
            dto.setDeliveryId(order.getDelivery().getId());
            dto.setDeliveryMethod(order.getDelivery().getType());
        }
        if (order.getStatus() != null) {
            dto.setStatus(order.getStatus().name()); // Set status string in DTO
        }
        return dto;
    }
}
