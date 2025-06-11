package com.project.Fashion.controller;

import com.project.Fashion.dto.CheckOutRequestDto;
import com.project.Fashion.dto.OrderRequestDto;
import com.project.Fashion.dto.OrderResponseDto;
import com.project.Fashion.model.*;
import com.project.Fashion.service.OrderService;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.DeliveryRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.OrderNotFoundException;
import com.project.Fashion.exception.exceptions.InvalidFieldException;
import com.project.Fashion.exception.exceptions.ProductNotFoundException;
import com.project.Fashion.exception.exceptions.DeliveryNotFoundException;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(path = "/api/orders")
@Tag(name = "Order Management", description = "APIs for creating, viewing, and managing orders.")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;

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

    private User getAuthenticatedUserFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName();
        return userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
    }

    @Operation(summary = "Check if the authenticated user has purchased a product",
            description = "Returns true if the authenticated BUYER has a completed order for the given product ID.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully checked purchase status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", example = "{\"hasPurchased\": true}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/user/has-purchased/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Map<String, Boolean>> hasUserPurchasedProduct(@PathVariable Long productId) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
        boolean hasPurchased = orderService.checkIfUserHasPurchasedProduct(authenticatedUser.getId(), productId);
        return ResponseEntity.ok(Map.of("hasPurchased", hasPurchased));
    }

    @Operation(summary = "Create a new order (Buyer only)",
            description = "Allows a buyer to create a new order. The user ID in the request must match the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User, Product, or Delivery not found")
    })
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
        if (!authenticatedUser.getId().equals(request.getUser())) {
            throw new AccessDeniedException("User can only create orders for themselves.");
        }
        OrderResponseDto createdOrder = orderService.createOrderFromDto(request);
        return ResponseEntity.ok(createdOrder);
    }

    @Operation(summary = "Get all orders (Admin only)",
            description = "Retrieves a list of all orders. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> responseDtos = orderService.getAllOrders();
        return ResponseEntity.ok(responseDtos);
    }

    @Operation(summary = "Get orders for the authenticated buyer (Buyer only)",
            description = "Retrieves a paginated list of orders for the currently authenticated BUYER.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved buyer's orders"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination/sort parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)")
    })
    @GetMapping("/user/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Page<OrderResponseDto>> getMyOrders(Pageable pageable) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
        Page<OrderResponseDto> orders = orderService.getOrdersByUserId(authenticatedUser.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get orders for the authenticated seller (Seller only)",
            description = "Retrieves a paginated list of orders containing products sold by the currently authenticated SELLER. Supports sorting.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved seller's orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a SELLER)")
    })
    @GetMapping("/seller/me")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<OrderResponseDto>> getMyOrdersAsSeller(Pageable pageable) {
        Page<OrderResponseDto> orders = orderService.getOrdersForAuthenticatedSeller(pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get sales statistics for the authenticated seller",
            description = "Retrieves total sales figures for the currently authenticated SELLER based on paid orders.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved sales stats"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a SELLER)")
    })
    @GetMapping("/seller/me/stats")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Map<String, Double>> getMySalesStats() {
        double totalSales = orderService.getTotalSalesForAuthenticatedSeller();
        return ResponseEntity.ok(Map.of("totalSales", totalSales));
    }

    @Operation(summary = "Get a specific order by ID",
            description = "Retrieves details for a specific order. ADMINs can view any order. BUYERs can only view their own orders. SELLERs can only view orders containing their products.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved order details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'SELLER')")
    public ResponseEntity<OrderResponseDto> getOrder(@PathVariable Long id) {
        OrderResponseDto orderDto = orderService.getOrderById(id);
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
        String userRole = authenticatedUser.getRole().toUpperCase();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        switch (userRole) {
            case "ADMIN": break;
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
        return ResponseEntity.ok(orderDto);
    }

    @Operation(summary = "Get orders for a specific user (Admin or Buyer)",
            description = "Retrieves a paginated list of orders for a given user ID. ADMINs can view orders for any user. BUYERs can only view their own orders.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user's orders"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<Page<OrderResponseDto>> getOrdersByUserId(@PathVariable String userId, Pageable pageable) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
        String userRole = authenticatedUser.getRole().toUpperCase();
        if ("BUYER".equals(userRole) && !authenticatedUser.getId().equals(userId)) {
            throw new AccessDeniedException("Buyers can only view their own list of orders.");
        }
        Page<OrderResponseDto> responseDtos = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(responseDtos);
    }

    @Operation(summary = "Update an order (Admin only)",
            description = "Allows an ADMIN to fully update an order's details. Sellers should use PATCH for status updates.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin)"),
            @ApiResponse(responseCode = "404", description = "Order, User, Product, or Delivery not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto updatedOrder = orderService.updateOrder(id, orderRequestDto);
        return ResponseEntity.ok(updatedOrder);
    }

    @Operation(summary = "Partially update an order (Admin or Seller)",
            description = "ADMIN can patch any field. SELLER can only patch 'status' of an order containing their product.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order partially updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or field"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<OrderResponseDto> patchOrder(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
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
                if (!(statusValue instanceof String)) throw new InvalidFieldException("Order status must be a string.");
                try { OrderStatus.valueOf(((String) statusValue).toUpperCase()); }
                catch (IllegalArgumentException e) { throw new InvalidFieldException("Invalid order status value: " + statusValue); }
            }
        }
        OrderResponseDto patchedOrder = orderService.patchOrder(id, updates);
        return ResponseEntity.ok(patchedOrder);
    }

    @Operation(summary = "Delete an order (Admin only)",
            description = "Deletes an order. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Checkout items from cart (Buyer only)",
            description = "Converts items in the authenticated buyer's cart into orders.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout successful, orders created"),
            @ApiResponse(responseCode = "400", description = "Invalid input (e.g., cart empty)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User or Delivery not found")
    })
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<OrderResponseDto>> checkout(@Valid @RequestBody CheckOutRequestDto request) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
        if (!authenticatedUser.getId().equals(request.getUserId())) {
            throw new AccessDeniedException("User can only checkout their own cart.");
        }
        List<OrderResponseDto> responseDtos = orderService.checkout(request.getUserId(), request.getDeliveryId());
        return ResponseEntity.ok(responseDtos);
    }
}