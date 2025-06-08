package com.project.Fashion.controller;

import com.project.Fashion.dto.ProductResponseDto;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.service.FavoriteService;
import com.project.Fashion.exception.exceptions.UserNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me/favorites") // Base path for authenticated user's favorites
@Tag(name = "Favorite Management", description = "APIs for managing a user's favorite products.")
@SecurityRequirement(name = "bearerAuth") // All favorite operations require authentication
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    @Autowired
    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

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

    @Operation(summary = "Get all favorite products for the authenticated user",
            description = "Retrieves a list of all products that the currently authenticated BUYER has marked as favorite.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved favorite products",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProductResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)"),
            @ApiResponse(responseCode = "404", description = "Authenticated user not found (should not happen if token is valid and user exists)")
    })
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<ProductResponseDto>> getMyFavorites() {
        String userId = getAuthenticatedUserId();
        List<ProductResponseDto> favoriteProducts = favoriteService.getFavoriteProductsByUserId(userId);
        return ResponseEntity.ok(favoriteProducts);
    }

    @Operation(summary = "Add a product to the authenticated user's favorites",
            description = "Allows the currently authenticated BUYER to add a product to their list of favorites.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product successfully added to favorites"),
            @ApiResponse(responseCode = "400", description = "Invalid request (e.g., if product is already a favorite and service layer prevents duplicates explicitly, though current service is idempotent on add)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)"),
            @ApiResponse(responseCode = "404", description = "Authenticated user or Product not found")
    })
    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> addFavorite(
            @Parameter(description = "ID of the product to add to favorites", required = true, example = "1")
            @PathVariable Long productId) {
        String userId = getAuthenticatedUserId();
        favoriteService.addFavorite(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove a product from the authenticated user's favorites",
            description = "Allows the currently authenticated BUYER to remove a product from their list of favorites.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product successfully removed from favorites"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)"),
            @ApiResponse(responseCode = "404", description = "Authenticated user or Product not found (or favorite entry not found, though service is idempotent on delete)")
    })
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> removeFavorite(
            @Parameter(description = "ID of the product to remove from favorites", required = true, example = "1")
            @PathVariable Long productId) {
        String userId = getAuthenticatedUserId();
        favoriteService.removeFavorite(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if a product is favorited by the authenticated user",
            description = "Checks whether a specific product is in the authenticated BUYER's list of favorites and returns a boolean status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully checked favorite status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", example = "{\"isFavorited\": true}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)"),
            @ApiResponse(responseCode = "404", description = "Authenticated user or Product not found (if strict checks were in place before `existsBy...` call)")
    })
    @GetMapping("/status/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Map<String, Boolean>> isProductFavorited(
            @Parameter(description = "ID of the product to check favorite status for", required = true, example = "1")
            @PathVariable Long productId) {
        String userId = getAuthenticatedUserId();
        boolean isFavorited = favoriteService.isProductFavoritedByUser(userId, productId);
        return ResponseEntity.ok(Map.of("isFavorited", isFavorited));
    }
}
