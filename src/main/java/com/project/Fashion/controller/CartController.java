package com.project.Fashion.controller;

import com.project.Fashion.dto.CartRequestDto;
import com.project.Fashion.dto.CartResponseDto;
import com.project.Fashion.dto.CartItemQuantityUpdateDto;
import com.project.Fashion.model.Cart;
import com.project.Fashion.model.User;
import com.project.Fashion.service.CartService;
// CartRepository is not strictly needed here anymore if all auth checks are based on authenticated user
// and service layer handles fetches by ID.
// import com.project.Fashion.repository.CartRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.CartNotFoundException;
import com.project.Fashion.exception.exceptions.InvalidFieldException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;
    // private final CartRepository cartRepository; // Can be removed if not directly used

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

    @PostMapping // No path, defaults to /api/cart
    public ResponseEntity<CartResponseDto> addCart(@RequestBody @Valid CartRequestDto cartRequestDto) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(cartRequestDto.getUserId())) {
            throw new AccessDeniedException("User can only add items to their own cart.");
        }
        // Quantity validation is now also in service, but good to keep here for early fail
        if (cartRequestDto.getQuantity() <= 0) {
            throw new InvalidFieldException("Quantity must be positive.");
        }

        return ResponseEntity.ok(cartService.addCart(cartRequestDto));
    }

    @GetMapping("/user/me") // Changed from /user/{userId} to /user/me for fetching own cart
    public ResponseEntity<List<CartResponseDto>> getMyCart() {
        User authenticatedUser = getAuthenticatedUser();
        return ResponseEntity.ok(cartService.getCartDtosByUser(authenticatedUser.getId()));
    }

    // Get a specific cart item by its ID (cart_item_id)
    @GetMapping("/{cartItemId}")
    public ResponseEntity<CartResponseDto> getCartItem(@PathVariable Long cartItemId) {
        User authenticatedUser = getAuthenticatedUser();
        Cart cart = cartService.getCartById(cartItemId);

        if (cart.getUser() == null || !cart.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only view their own cart items.");
        }

        return ResponseEntity.ok(cartService.convertToDTO(cart));
    }

    @PutMapping("/{cartItemId}/quantity")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @RequestBody @Valid CartItemQuantityUpdateDto quantityUpdateDto) {

        User authenticatedUser = getAuthenticatedUser();
        Cart existingCartItem = cartService.getCartById(cartItemId);

        if (existingCartItem.getUser() == null || !existingCartItem.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only update their own cart items.");
        }

        // Quantity validation (e.g. @Min(1)) is on DTO

        Cart updatedCart = cartService.updateCartItemQuantity(cartItemId, quantityUpdateDto.getQuantity());

        return ResponseEntity.ok(cartService.convertToDTO(updatedCart));
    }

    @PatchMapping("/{cartItemId}")
    public ResponseEntity<CartResponseDto> patchCartItem(
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Object> updates) { // Keep generic patch for flexibility if needed
        User authenticatedUser = getAuthenticatedUser();
        Cart existingCartItem = cartService.getCartById(cartItemId);

        if (existingCartItem.getUser() == null || !existingCartItem.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only patch their own cart items.");
        }

        // Validate that only "quantity" is being patched, as per previous logic
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
                // Service layer's patchCart now also throws if quantity <=0
                throw new InvalidFieldException("Quantity must be positive. To remove, use DELETE.");
            }
        }

        Cart patchedCart = cartService.patchCart(cartItemId, updates);
        return ResponseEntity.ok(cartService.convertToDTO(patchedCart));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long cartItemId) {
        User authenticatedUser = getAuthenticatedUser();
        Cart existingCartItem = cartService.getCartById(cartItemId);

        if (existingCartItem.getUser() == null || !existingCartItem.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only delete their own cart items.");
        }

        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/me/clear") // New endpoint to clear the authenticated user's cart
    public ResponseEntity<Void> clearMyCart() {
        User authenticatedUser = getAuthenticatedUser();
        cartService.clearUserCart(authenticatedUser.getId());
        return ResponseEntity.noContent().build();
    }
}
