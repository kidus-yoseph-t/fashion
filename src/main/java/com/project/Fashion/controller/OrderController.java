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
import io.swagger.v3.oas.annotations.Parameter; // Import Parameter
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
import java.util.stream.Collectors;

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

    private User getAuthenticatedUserFromSecurityContext() { // Renamed to avoid conflict if inherited
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName();
        return userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
    }

    @Operation(summary = "Create a new order (Buyer only)",
            description = "Allows a buyer to create a new order. The user ID in the request must match the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrderResponseDto.class))),
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
        Order order = orderService.createOrderFromDto(request);
        OrderResponseDto responseDto = orderService.convertToDto(order);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "Get all orders (Admin only)",
            description = "Retrieves a list of all orders. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all orders",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = OrderResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        List<OrderResponseDto> responseDtos = orders.stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @Operation(summary = "Get orders for the authenticated seller (Seller only)",
            description = "Retrieves a paginated list of orders containing products sold by the currently authenticated SELLER. Supports sorting.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved seller's orders",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Page.class))), // Page<OrderResponseDto>
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a SELLER)")
    })
    @GetMapping("/seller/me")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<OrderResponseDto>> getMyOrdersAsSeller(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., date, total, status). Default: date.", example = "date") @RequestParam(required = false, defaultValue = "date") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC). Default: DESC for date.", example = "DESC") @RequestParam(required = false, defaultValue = "DESC") String sortDir
    ) {
        Sort.Direction direction = Sort.Direction.ASC;
        if (sortDir.equalsIgnoreCase("DESC")) {
            direction = Sort.Direction.DESC;
        }
        // Validate sortBy field for orders (e.g., "date", "total", "status", "product.name")
        // For simplicity, allowing common fields. Add more robust validation if needed.
        List<String> validSortProperties = List.of("date", "total", "status", "id", "quantity", "product.name", "user.email");
        String sortProperty = validSortProperties.contains(sortBy) ? sortBy : "date"; // Default sort for orders is usually date

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
        Page<OrderResponseDto> orders = orderService.getOrdersForAuthenticatedSeller(pageable);
        return ResponseEntity.ok(orders);
    }


    @Operation(summary = "Get a specific order by ID",
            description = "Retrieves details for a specific order. ADMINs can view any order. BUYERs can only view their own orders. SELLERs can only view orders containing their products.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved order details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'SELLER')")
    public ResponseEntity<OrderResponseDto> getOrder(@PathVariable Long id) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        String userRole = authenticatedUser.getRole().toUpperCase();

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
        return ResponseEntity.ok(orderService.convertToDto(order));
    }

    @Operation(summary = "Get orders for a specific user (Admin or Buyer)",
            description = "Retrieves all orders for a given user ID. ADMINs can view orders for any user. BUYERs can only view their own orders.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user's orders",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = OrderResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByUserId(@PathVariable String userId) {
        User authenticatedUser = getAuthenticatedUserFromSecurityContext();
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

    @Operation(summary = "Update an order (Admin only)",
            description = "Allows an ADMIN to fully update an order's details. Sellers should use PATCH for status updates.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin)"),
            @ApiResponse(responseCode = "404", description = "Order, User, Product, or Delivery not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderRequestDto orderRequestDto) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        User orderUser = userRepository.findById(orderRequestDto.getUser())
                .orElseThrow(() -> new UserNotFoundException("User for order not found: " + orderRequestDto.getUser()));
        Product orderProduct = productRepository.findById(orderRequestDto.getProduct())
                .orElseThrow(() -> new ProductNotFoundException("Product for order not found: " + orderRequestDto.getProduct()));
        Delivery orderDelivery = deliveryRepository.findById(orderRequestDto.getDelivery())
                .orElseThrow(() -> new DeliveryNotFoundException("Delivery for order not found: " + orderRequestDto.getDelivery()));
        existingOrder.setUser(orderUser);
        existingOrder.setProduct(orderProduct);
        existingOrder.setDate(orderRequestDto.getDate());
        existingOrder.setQuantity(orderRequestDto.getQuantity());
        existingOrder.setTotal(orderRequestDto.getTotal());
        existingOrder.setDelivery(orderDelivery);
        Order updatedOrder = orderService.updateOrder(id, existingOrder);
        return ResponseEntity.ok(orderService.convertToDto(updatedOrder));
    }

    @Operation(summary = "Partially update an order (Admin or Seller)",
            description = "ADMIN can patch any field. SELLER can only patch 'status' of an order containing their product.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order partially updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrderResponseDto.class))),
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
        Order patchedOrder = orderService.patchOrder(id, updates);
        return ResponseEntity.ok(orderService.convertToDto(patchedOrder));
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
            @ApiResponse(responseCode = "200", description = "Checkout successful, orders created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = OrderResponseDto.class)))),
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
        List<Order> orders = orderService.checkout(request.getUserId(), request.getDeliveryId());
        List<OrderResponseDto> responseDtos = orders.stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }
}
