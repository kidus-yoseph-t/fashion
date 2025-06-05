package com.project.Fashion.service;

import com.project.Fashion.dto.OrderRequestDto;
import com.project.Fashion.dto.OrderResponseDto;
import com.project.Fashion.exception.exceptions.*;
import com.project.Fashion.model.Cart;
import com.project.Fashion.model.Delivery;
import com.project.Fashion.model.Order;
import com.project.Fashion.model.OrderStatus;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.CartRepository;
import com.project.Fashion.repository.DeliveryRepository;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final DeliveryRepository deliveryRepository;

    // Helper method to get current authenticated user (similar to ProductService)
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // This case should ideally be caught by security filters,
            // but good to have a fallback.
            throw new AccessDeniedException("User is not authenticated.");
        }
        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        } else {
            // Should not happen with standard Spring Security setup
            throw new AccessDeniedException("Invalid principal type for authentication.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found in database. Email: " + email));
    }


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
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
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
        order.setDate(dto.getDate() != null ? dto.getDate() : new Date());
        order.setQuantity(dto.getQuantity());
        order.setTotal(dto.getTotal());
        order.setDelivery(delivery);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(String userId) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId + " when fetching orders."));
        return orderRepository.findByUserId(userId);
    }

    public Order updateOrder(Long id, Order updatedOrderDetails) {
        Order existingOrder = getOrder(id);
        if (updatedOrderDetails.getUser() != null && updatedOrderDetails.getUser().getId() != null) {
            User user = userRepository.findById(updatedOrderDetails.getUser().getId())
                    .orElseThrow(() -> new UserNotFoundException("User for order update: " + updatedOrderDetails.getUser().getId()));
            existingOrder.setUser(user);
        }
        if (updatedOrderDetails.getProduct() != null && updatedOrderDetails.getProduct().getId() != null) {
            Product product = productRepository.findById(updatedOrderDetails.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException("Product for order update: " + updatedOrderDetails.getProduct().getId()));
            existingOrder.setProduct(product);
        }
        if (updatedOrderDetails.getDelivery() != null && updatedOrderDetails.getDelivery().getId() != null) {
            Delivery delivery = deliveryRepository.findById(updatedOrderDetails.getDelivery().getId())
                    .orElseThrow(() -> new DeliveryNotFoundException("Delivery for order update: " + updatedOrderDetails.getDelivery().getId()));
            existingOrder.setDelivery(delivery);
        }
        existingOrder.setQuantity(updatedOrderDetails.getQuantity());
        existingOrder.setDate(updatedOrderDetails.getDate() != null ? updatedOrderDetails.getDate() : existingOrder.getDate());
        existingOrder.setTotal(updatedOrderDetails.getTotal());
        if (updatedOrderDetails.getStatus() != null) {
            existingOrder.setStatus(updatedOrderDetails.getStatus());
        }
        return orderRepository.save(existingOrder);
    }

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
                case "status" -> {
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

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Order not found with ID: " + id);
        }
        orderRepository.deleteById(id);
    }

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
            order.setDate(new Date());
            order.setTotal(cart.getProduct().getPrice() * cart.getQuantity());
            order.setDelivery(delivery);
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            orders.add(orderRepository.save(order));
        }
        cartRepository.deleteAll(cartItems);
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
            dto.setProductPrice(order.getProduct().getPrice());
        }
        dto.setDate(order.getDate());
        dto.setQuantity(order.getQuantity());
        dto.setTotal(order.getTotal());
        if (order.getDelivery() != null) {
            dto.setDeliveryId(order.getDelivery().getId());
            dto.setDeliveryMethod(order.getDelivery().getType());
        }
        if (order.getStatus() != null) {
            dto.setStatus(order.getStatus().name());
        }
        return dto;
    }

    /**
     * Retrieves a paginated list of orders containing products sold by the currently authenticated seller.
     * @param pageable Pagination and sorting information.
     * @return A Page of OrderResponseDto.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrdersForAuthenticatedSeller(Pageable pageable) {
        User authenticatedSeller = getCurrentAuthenticatedUser();
        // Ensure the user is indeed a seller, though @PreAuthorize on controller is primary guard
        if (!"SELLER".equalsIgnoreCase(authenticatedSeller.getRole())) {
            log.warn("User {} with role {} attempted to access seller-specific orders.", authenticatedSeller.getEmail(), authenticatedSeller.getRole());
            throw new AccessDeniedException("Only users with SELLER role can access this resource.");
        }

        log.info("Fetching orders for authenticated seller: {} with pagination: {}", authenticatedSeller.getEmail(), pageable);
        Page<Order> ordersPage = orderRepository.findByProduct_Seller_Id(authenticatedSeller.getId(), pageable);
        return ordersPage.map(this::convertToDto);
    }
}
