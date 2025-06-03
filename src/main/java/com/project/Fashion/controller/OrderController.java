package com.project.Fashion.controller;

import com.project.Fashion.dto.CheckOutRequestDto;
import com.project.Fashion.dto.OrderRequestDto;
import com.project.Fashion.dto.OrderResponseDto;
import com.project.Fashion.model.Order;
import com.project.Fashion.model.OrderStatus;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.service.OrderService;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.repository.UserRepository;
// Assuming Delivery and Product might be needed for constructing Order from DTO for update
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.DeliveryRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.OrderNotFoundException;
import com.project.Fashion.exception.exceptions.InvalidFieldException;
import com.project.Fashion.exception.exceptions.ProductNotFoundException;
import com.project.Fashion.exception.exceptions.DeliveryNotFoundException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository; // Added for PUT update
    private final DeliveryRepository deliveryRepository; // Added for PUT update

    private static final Set<String> ALLOWED_SELLER_PATCH_FIELDS = Set.of("status");


    @Autowired
    public OrderController(OrderService orderService,
                           UserRepository userRepository,
                           OrderRepository orderRepository,
                           ProductRepository productRepository,
                           DeliveryRepository deliveryRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.deliveryRepository = deliveryRepository;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName();
        return userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody OrderRequestDto request) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(request.getUser())) {
            throw new AccessDeniedException("User can only create orders for themselves.");
        }

        Order order = orderService.createOrderFromDto(request);
        OrderResponseDto responseDto = orderService.convertToDto(order);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        List<OrderResponseDto> responseDtos = orders.stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrder(@PathVariable Long id) {
        User authenticatedUser = getAuthenticatedUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        String userRole = authenticatedUser.getRole().toUpperCase();

        switch (userRole) {
            case "ADMIN":
                break;
            case "BUYER":
                if (order.getUser() == null || !order.getUser().getId().equals(authenticatedUser.getId())) {
                    throw new AccessDeniedException("Buyers can only view their own orders.");
                }
                break;
            case "SELLER":
                Product productInOrder = order.getProduct();
                if (productInOrder == null || productInOrder.getSeller() == null ||
                        !productInOrder.getSeller().getId().equals(authenticatedUser.getId())) {
                    throw new AccessDeniedException("Sellers can only view orders containing their products.");
                }
                break;
            default:
                throw new AccessDeniedException("User role not recognized for accessing orders.");
        }

        return ResponseEntity.ok(orderService.convertToDto(order));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByUserId(@PathVariable String userId) {
        User authenticatedUser = getAuthenticatedUser();
        String userRole = authenticatedUser.getRole().toUpperCase();

        if ("BUYER".equals(userRole) && !authenticatedUser.getId().equals(userId)) {
            throw new AccessDeniedException("Buyers can only view their own list of orders.");
        }

        List<Order> userOrders = orderService.getOrdersByUserId(userId);
        List<OrderResponseDto> responseDtos = userOrders.stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long id, @RequestBody OrderRequestDto orderRequestDto) {
        User authenticatedUser = getAuthenticatedUser();
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        String userRole = authenticatedUser.getRole().toUpperCase();

        if (!"ADMIN".equals(userRole)) {
            if ("SELLER".equals(userRole)) {
                Product productInOrder = existingOrder.getProduct();
                if (productInOrder == null || productInOrder.getSeller() == null ||
                        !productInOrder.getSeller().getId().equals(authenticatedUser.getId())) {
                    throw new AccessDeniedException("Sellers cannot update orders not containing their products.");
                }
                throw new AccessDeniedException("Sellers should use the PATCH endpoint for specific updates like order status.");
            } else {
                throw new AccessDeniedException("User does not have permission to fully update this order.");
            }
        }

        // Admin full update logic:
        User orderUser = userRepository.findById(orderRequestDto.getUser())
                .orElseThrow(() -> new UserNotFoundException("User for order not found: " + orderRequestDto.getUser()));

        Product orderProduct = productRepository.findById(orderRequestDto.getProduct())
                .orElseThrow(() -> new ProductNotFoundException("Product for order not found: " + orderRequestDto.getProduct()));

        com.project.Fashion.model.Delivery orderDelivery = deliveryRepository.findById(orderRequestDto.getDelivery())
                .orElseThrow(() -> new DeliveryNotFoundException("Delivery for order not found: " + orderRequestDto.getDelivery()));

        existingOrder.setUser(orderUser);
        existingOrder.setProduct(orderProduct);
        existingOrder.setDate(orderRequestDto.getDate());
        existingOrder.setQuantity(orderRequestDto.getQuantity());
        existingOrder.setTotal(orderRequestDto.getTotal());
        existingOrder.setDelivery(orderDelivery);

        // Status update logic is REMOVED from PUT as per Option 2.
        // Status will be updated via PATCH.
        // If a default status needs to be set on creation or if Admins should set it via PUT,
        // OrderRequestDto would need 'status' and logic here to set it from DTO,
        // but we chose Option 2 to keep status out of this DTO for updates.

        Order updatedOrder = orderService.updateOrder(id, existingOrder);
        return ResponseEntity.ok(orderService.convertToDto(updatedOrder));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponseDto> patchOrder(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        User authenticatedUser = getAuthenticatedUser();
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        String userRole = authenticatedUser.getRole().toUpperCase();

        if ("SELLER".equals(userRole)) {
            Product productInOrder = existingOrder.getProduct();
            if (productInOrder == null || productInOrder.getSeller() == null ||
                    !productInOrder.getSeller().getId().equals(authenticatedUser.getId())) {
                throw new AccessDeniedException("Sellers can only patch orders containing their products.");
            }

            for (String key : updates.keySet()) {
                if (!ALLOWED_SELLER_PATCH_FIELDS.contains(key)) {
                    throw new AccessDeniedException("Sellers are not allowed to update field: '" + key + "'. Only " + ALLOWED_SELLER_PATCH_FIELDS + " are permitted.");
                }
            }
            if (updates.containsKey("status")) {
                Object statusValue = updates.get("status");
                if (!(statusValue instanceof String)) {
                    throw new InvalidFieldException("Order status must be a string.");
                }
                try {
                    // The actual update of status will be handled by orderService.patchOrder
                    // This just validates the value if present.
                    OrderStatus.valueOf(((String) statusValue).toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new InvalidFieldException("Invalid order status value: " + statusValue);
                }
            }

        } else if (!"ADMIN".equals(userRole)) {
            throw new AccessDeniedException("User does not have permission to patch this order.");
        }

        Order patchedOrder = orderService.patchOrder(id, updates);
        return ResponseEntity.ok(orderService.convertToDto(patchedOrder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponseDto>> checkout(@RequestBody CheckOutRequestDto request) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(request.getUserId())) {
            throw new AccessDeniedException("User can only checkout their own cart.");
        }

        List<Order> orders = orderService.checkout(request.getUserId(), request.getDeliveryId());
        List<OrderResponseDto> responseDtos = orders.stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }
}
