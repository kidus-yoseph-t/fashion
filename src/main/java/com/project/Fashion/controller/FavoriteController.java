package com.project.Fashion.controller;

import com.project.Fashion.dto.ProductDto;
import com.project.Fashion.model.User; // To get authenticated user
import com.project.Fashion.repository.UserRepository; // To fetch User by email
import com.project.Fashion.service.FavoriteService;
import com.project.Fashion.exception.exceptions.UserNotFoundException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // For the boolean response

@RestController
@RequestMapping("/api/users/me/favorites") // Base path for authenticated user's favorites
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository; // To get the full User object for their ID

    @Autowired
    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    /**
     * Helper method to get the ID of the currently authenticated user.
     * @return String The authenticated user's ID.
     * @throws UserNotFoundException if the authenticated user cannot be found.
     * @throws AccessDeniedException if no user is authenticated.
     */
    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated. Cannot access favorites.");
        }
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + userEmail));
        return user.getId();
    }

    // Get all favorite products for the authenticated user
    @GetMapping
    @PreAuthorize("hasRole('BUYER')") // Only BUYERs can have favorites in this model
    public ResponseEntity<List<ProductDto>> getMyFavorites() {
        String userId = getAuthenticatedUserId();
        List<ProductDto> favoriteProducts = favoriteService.getFavoriteProductsByUserId(userId);
        return ResponseEntity.ok(favoriteProducts);
    }

    // Add a product to the authenticated user's favorites
    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> addFavorite(@PathVariable Long productId) {
        String userId = getAuthenticatedUserId();
        favoriteService.addFavorite(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created is appropriate for adding
    }

    // Remove a product from the authenticated user's favorites
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long productId) {
        String userId = getAuthenticatedUserId();
        favoriteService.removeFavorite(userId, productId);
        return ResponseEntity.noContent().build(); // 204 No Content is appropriate for successful deletion
    }

    // Check if a specific product is favorited by the authenticated user
    @GetMapping("/status/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Map<String, Boolean>> isProductFavorited(@PathVariable Long productId) {
        String userId = getAuthenticatedUserId();
        boolean isFavorited = favoriteService.isProductFavoritedByUser(userId, productId);
        return ResponseEntity.ok(Map.of("isFavorited", isFavorited));
    }
}
