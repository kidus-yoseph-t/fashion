package com.project.Fashion.controller;

import com.project.Fashion.dto.CartRequestDto;
import com.project.Fashion.dto.CartResponseDto;
import com.project.Fashion.dto.CartItemQuantityUpdateDto;
import com.project.Fashion.model.Cart;
import com.project.Fashion.model.User;
import com.project.Fashion.service.CartService;
import com.project.Fashion.repository.CartRepository;
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
@RequestMapping(path = "/api/cart", produces = MediaType.APPLICATION_JSON_VALUE)
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    @Autowired
    public CartController(CartService cartService,
                          UserRepository userRepository,
                          CartRepository cartRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping("/{id}")
    public ResponseEntity<CartResponseDto> getCart(@PathVariable Long id) {
        User authenticatedUser = getAuthenticatedUser();
        // Fetch Cart entity using service method that takes Long
        Cart cart = cartService.getCartById(id); // Assuming CartService.getCartById(Long) exists

        if (cart.getUser() == null || !cart.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only view their own cart details.");
        }

        return ResponseEntity.ok(cartService.convertToDTO(cart));
    }

    @PutMapping("/{id}/quantity")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @PathVariable Long id,
            @RequestBody @Valid CartItemQuantityUpdateDto quantityUpdateDto) {

        User authenticatedUser = getAuthenticatedUser();
        // Fetch cart to check ownership before attempting update
        Cart existingCart = cartService.getCartById(id);

        if (existingCart.getUser() == null || !existingCart.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only update their own cart items.");
        }

        if (quantityUpdateDto.getQuantity() <= 0) {
            throw new InvalidFieldException("Quantity must be positive.");
        }

        Cart updatedCart = cartService.updateCartItemQuantity(id, quantityUpdateDto.getQuantity());

        return ResponseEntity.ok(cartService.convertToDTO(updatedCart));
    }

    @PatchMapping("/{id}/quantity")
    public ResponseEntity<CartResponseDto> patchCartItemQuantity(
            @PathVariable Long id,
            @RequestBody @Valid CartItemQuantityUpdateDto quantityUpdateDto) {
        User authenticatedUser = getAuthenticatedUser();
        Cart existingCart = cartService.getCartById(id);

        if (existingCart.getUser() == null || !existingCart.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only patch their own cart items.");
        }

        if (quantityUpdateDto.getQuantity() <= 0) {
            throw new InvalidFieldException("Quantity must be positive.");
        }

        Cart patchedCart = cartService.updateCartItemQuantity(id, quantityUpdateDto.getQuantity());
        return ResponseEntity.ok(cartService.convertToDTO(patchedCart));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CartResponseDto> patchCart(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        User authenticatedUser = getAuthenticatedUser();
        Cart existingCart = cartService.getCartById(id);

        if (existingCart.getUser() == null || !existingCart.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only patch their own cart items.");
        }

        for (String key : updates.keySet()) {
            if (!key.equals("quantity")) {
                throw new AccessDeniedException("Field '" + key + "' cannot be updated via this patch endpoint. Only 'quantity' is allowed. Use specific endpoints for other fields if available.");
            }
        }

        if (updates.containsKey("quantity")) {
            Object quantityObj = updates.get("quantity");
            if (!(quantityObj instanceof Integer)) {
                throw new InvalidFieldException("Quantity must be an integer.");
            }
            if ((Integer) quantityObj <= 0) {
                throw new InvalidFieldException("Quantity must be positive.");
            }
        }

        Cart patchedCart = cartService.patchCart(id, updates);
        return ResponseEntity.ok(cartService.convertToDTO(patchedCart));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long id) {
        User authenticatedUser = getAuthenticatedUser();
        Cart existingCart = cartService.getCartById(id);

        if (existingCart.getUser() == null || !existingCart.getUser().getId().equals(authenticatedUser.getId())) {
            throw new AccessDeniedException("User can only delete their own cart items.");
        }

        cartService.deleteCart(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartResponseDto>> getCartsByUser(@PathVariable String userId) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(userId)) {
            throw new AccessDeniedException("User can only retrieve their own list of cart items.");
        }

        return ResponseEntity.ok(cartService.getCartDtosByUser(userId));
    }
}
