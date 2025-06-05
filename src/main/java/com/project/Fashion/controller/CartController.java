package com.project.Fashion.controller;

import com.project.Fashion.dto.CartRequestDto;
import com.project.Fashion.dto.CartResponseDto;
import com.project.Fashion.dto.CartItemQuantityUpdateDto;
import com.project.Fashion.model.Cart;
import com.project.Fashion.model.User;
import com.project.Fashion.service.CartService;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.CartNotFoundException;
import com.project.Fashion.exception.exceptions.InvalidFieldException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/cart")
@Tag(name = "Cart Management", description = "APIs for managing user shopping carts.")
@SecurityRequirement(name = "bearerAuth") // All cart operations require authentication
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @Autowired
    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
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

    @Operation(summary = "Add an item to the cart or update its quantity",
            description = "Adds a specified quantity of a product to the authenticated BUYER's cart. If the product is already in the cart, its quantity is increased. User ID in the request must match the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added/updated in cart successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CartResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., quantity <= 0, product/user not found)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"Quantity must be positive.\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER or trying to add to another user's cart)"),
            @ApiResponse(responseCode = "404", description = "User or Product not found")
    })
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponseDto> addCart(@RequestBody @Valid CartRequestDto cartRequestDto) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(cartRequestDto.getUserId())) {
            throw new AccessDeniedException("User can only add items to their own cart.");
        }
        if (cartRequestDto.getQuantity() <= 0) {
            throw new InvalidFieldException("Quantity must be positive.");
        }

        return ResponseEntity.ok(cartService.addCart(cartRequestDto));
    }

    @Operation(summary = "Get the authenticated user's cart",
            description = "Retrieves all items currently in the authenticated BUYER's shopping cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved cart items",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CartResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)"),
            @ApiResponse(responseCode = "404", description = "User not found (should not happen if authenticated)")
    })
    @GetMapping("/user/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<CartResponseDto>> getMyCart() {
        User authenticatedUser = getAuthenticatedUser();
        return ResponseEntity.ok(cartService.getCartDtosByUser(authenticatedUser.getId()));
    }

    @Operation(summary = "Get a specific cart item by its ID",
            description = "Retrieves details for a specific item in the authenticated BUYER's cart using the cart item's unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved cart item details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CartResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER or item does not belong to user)"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @GetMapping("/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponseDto> getCartItem(@PathVariable Long cartItemId) {
        User authenticatedUser = getAuthenticatedUser();
        Cart cart = cartService.getCartById(cartItemId); // Service throws CartNotFoundException if not found

        if (cart.getUser() == null || !cart.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only view their own cart items.");
        }

        return ResponseEntity.ok(cartService.convertToDTO(cart));
    }

    @Operation(summary = "Update the quantity of a specific cart item",
            description = "Updates the quantity of an item in the authenticated BUYER's cart. The quantity must be at least 1.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart item quantity updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CartResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (e.g., quantity less than 1)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"quantity\":\"Quantity must be at least 1.\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER or item does not belong to user)"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @PutMapping("/{cartItemId}/quantity")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @RequestBody @Valid CartItemQuantityUpdateDto quantityUpdateDto) {

        User authenticatedUser = getAuthenticatedUser();
        Cart existingCartItem = cartService.getCartById(cartItemId);

        if (existingCartItem.getUser() == null || !existingCartItem.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only update their own cart items.");
        }
        // @Valid on DTO handles quantity validation (e.g. @Min(1))
        Cart updatedCart = cartService.updateCartItemQuantity(cartItemId, quantityUpdateDto.getQuantity());
        return ResponseEntity.ok(cartService.convertToDTO(updatedCart));
    }

    @Operation(summary = "Partially update a cart item (typically quantity)",
            hidden = true, // Hiding this as PUT /quantity is preferred and more specific.
            // If PATCH is truly needed for other fields in the future, this can be unhidden and refined.
            description = "Partially updates details of a specific item in the authenticated BUYER's cart. Currently, only 'quantity' is supported. Quantity must be positive.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart item partially updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CartResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or field (e.g., non-integer quantity, quantity <= 0, unsupported field)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"Quantity must be an integer.\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not BUYER or item not theirs)"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @PatchMapping("/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartResponseDto> patchCartItem(
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Object> updates) {
        User authenticatedUser = getAuthenticatedUser();
        Cart existingCartItem = cartService.getCartById(cartItemId);

        if (existingCartItem.getUser() == null || !existingCartItem.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only patch their own cart items.");
        }

        for (String key : updates.keySet()) {
            if (!key.equals("quantity")) {
                throw new AccessDeniedException("Field '" + key + "' cannot be updated via this patch endpoint. Only 'quantity' is allowed.");
            }
        }

        if (updates.containsKey("quantity")) {
            Object quantityObj = updates.get("quantity");
            if (!(quantityObj instanceof Integer)) {
                throw new InvalidFieldException("Quantity must be an integer.");
            }
            if ((Integer) quantityObj <= 0) {
                throw new InvalidFieldException("Quantity must be positive. To remove, use DELETE.");
            }
        }

        Cart patchedCart = cartService.patchCart(cartItemId, updates);
        return ResponseEntity.ok(cartService.convertToDTO(patchedCart));
    }

    @Operation(summary = "Remove an item from the cart",
            description = "Deletes a specific item from the authenticated BUYER's shopping cart using the cart item's ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cart item removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER or item does not belong to user)"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @DeleteMapping("/{cartItemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long cartItemId) {
        User authenticatedUser = getAuthenticatedUser();
        Cart existingCartItem = cartService.getCartById(cartItemId); // Ensures item exists

        if (existingCartItem.getUser() == null || !existingCartItem.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only delete their own cart items.");
        }

        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Clear all items from the authenticated user's cart",
            description = "Removes all items from the authenticated BUYER's shopping cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)"),
            @ApiResponse(responseCode = "404", description = "User not found (should not happen if authenticated)")
    })
    @DeleteMapping("/user/me/clear")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> clearMyCart() {
        User authenticatedUser = getAuthenticatedUser();
        cartService.clearUserCart(authenticatedUser.getId());
        return ResponseEntity.noContent().build();
    }
}
