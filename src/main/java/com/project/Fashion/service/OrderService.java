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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // This helper method is well-implemented. No changes needed.
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        } else {
            throw new AccessDeniedException("Invalid principal type for authentication.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found in database. Email: " + email));
    }

    // This method is well-implemented. No changes needed.
    public OrderResponseDto  createOrderFromDto(OrderRequestDto dto) {
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

        Order savedOrder = orderRepository.save(order);
        return convertToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // No changes needed.
    @Cacheable(value = "order", key = "#id")
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        log.info("Fetching order from DB with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        return convertToDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrdersByUserId(String userId, Pageable pageable) {
        // Ensure the user exists before attempting to fetch orders
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId + " when fetching orders."));

        // Use the new repository method that accepts a Pageable object
        Page<Order> userOrdersPage = orderRepository.findByUserId(userId, pageable);

        // Convert the Page of entities to a Page of DTOs
        return userOrdersPage.map(this::convertToDto);
    }

    @CacheEvict(value = "order", key = "#id")
    public OrderResponseDto updateOrder(Long id, OrderRequestDto orderRequestDto) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
        if (orderRequestDto.getUser() != null) {
            User user = userRepository.findById(orderRequestDto.getUser())
                    .orElseThrow(() -> new UserNotFoundException("User for order update not found: " + orderRequestDto.getUser()));
            existingOrder.setUser(user);
        }
        if (orderRequestDto.getProduct() != null) {
            Product product = productRepository.findById(orderRequestDto.getProduct())
                    .orElseThrow(() -> new ProductNotFoundException("Product for order update not found: " + orderRequestDto.getProduct()));
            existingOrder.setProduct(product);
        }
        if (orderRequestDto.getDelivery() != null) {
            Delivery delivery = deliveryRepository.findById(orderRequestDto.getDelivery())
                    .orElseThrow(() -> new DeliveryNotFoundException("Delivery for order update not found: " + orderRequestDto.getDelivery()));
            existingOrder.setDelivery(delivery);
        }
        existingOrder.setQuantity(orderRequestDto.getQuantity());
        existingOrder.setDate(orderRequestDto.getDate() != null ? orderRequestDto.getDate() : existingOrder.getDate());
        existingOrder.setTotal(orderRequestDto.getTotal());
        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("Order {} updated. Evicting from 'order' cache.", id);
        return convertToDto(updatedOrder);
    }

    // This method is very well-implemented. No changes needed.
    @CacheEvict(value = "order", key = "#id")
    public OrderResponseDto patchOrder(Long id, Map<String, Object> updates) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + id));
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
                            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + value));
                    order.setUser(user);
                }
                case "productId" -> {
                    if (value == null) throw new InvalidFieldException("Product ID cannot be null.");
                    Product product = productRepository.findById(Long.parseLong(value.toString()))
                            .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + value));
                    order.setProduct(product);
                }
                case "deliveryId" -> {
                    if (value == null) throw new InvalidFieldException("Delivery ID cannot be null.");
                    Delivery delivery = deliveryRepository.findById(Long.parseLong(value.toString()))
                            .orElseThrow(() -> new DeliveryNotFoundException("Delivery not found with ID: " + value));
                    order.setDelivery(delivery);
                }
                case "status" -> {
                    if (value == null) throw new InvalidFieldException("Status cannot be null.");
                    try {
                        order.setStatus(OrderStatus.valueOf(value.toString().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new InvalidFieldException("Invalid status value: " + value);
                    }
                }
                default -> throw new InvalidFieldException("Invalid field for order patch: " + key);
            }
        });
        Order patchedOrder = orderRepository.save(order);
        log.info("Order {} patched. Evicting from 'order' cache.", id);
        return convertToDto(patchedOrder);
    }

    // No changes needed.
    @CacheEvict(value = "order", key = "#id")
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Order not found with ID: " + id);
        }
        orderRepository.deleteById(id);
        log.info("Order {} deleted. Evicting from 'order' cache.", id);
    }

    // --- REFACTORED METHOD START ---
    @Transactional
    public List<OrderResponseDto> checkout(String userId, Long deliveryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException("Delivery option not found with ID: " + deliveryId));
        List<Cart> cartItems = cartRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new CartEmptyException("Cannot checkout with an empty cart.");
        }

        List<Order> createdOrders = new ArrayList<>();

        for (Cart cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Inventory check
            if (product.getStock() < cartItem.getQuantity()) {
                // Throws an exception that will be translated to a 409 Conflict with a clear message
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for '" + product.getName() + "'. Requested: " + cartItem.getQuantity() + ", Available: " + product.getStock());
            }

            // Decrease the product stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product); // Save the updated stock level

            Order order = new Order();
            order.setUser(user);
            order.setProduct(product);
            order.setQuantity(cartItem.getQuantity());
            order.setDate(new Date());
            order.setTotal(product.getPrice() * cartItem.getQuantity()); // Recalculate total for safety
            order.setDelivery(delivery);
            order.setStatus(OrderStatus.PENDING_PAYMENT); // Set initial status

            createdOrders.add(orderRepository.save(order));
        }

        // Clear the user's cart after all orders are successfully created
        cartRepository.deleteAll(cartItems);

        log.info("Checkout successful for user ID: {}. Created {} orders.", userId, createdOrders.size());

        return createdOrders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrdersForAuthenticatedSeller(Pageable pageable) {
        User authenticatedSeller = getCurrentAuthenticatedUser();
        if (!"SELLER".equalsIgnoreCase(authenticatedSeller.getRole())) {
            log.warn("User {} with role {} attempted to access seller-specific orders.", authenticatedSeller.getEmail(), authenticatedSeller.getRole());
            throw new AccessDeniedException("Only users with SELLER role can access this resource.");
        }
        log.info("Fetching orders for authenticated seller: {} with pagination: {}", authenticatedSeller.getEmail(), pageable);
        Page<Order> ordersPage = orderRepository.findByProduct_Seller_Id(authenticatedSeller.getId(), pageable);
        return ordersPage.map(this::convertToDto);
    }
}