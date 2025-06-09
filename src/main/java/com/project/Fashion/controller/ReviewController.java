package com.project.Fashion.controller;

import com.project.Fashion.dto.ReviewDto;
import com.project.Fashion.model.Review;
import com.project.Fashion.model.User;
import com.project.Fashion.service.ReviewService;
import com.project.Fashion.repository.ReviewRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.ReviewNotFoundException;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Review Management", description = "APIs for managing product reviews.")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Autowired
    public ReviewController(ReviewService reviewService,
                            UserRepository userRepository,
                            ReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName(); // Email
        return userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
    }

    @Operation(summary = "Get all reviews for the authenticated user (Buyer only)",
            description = "Retrieves a list of all reviews submitted by the currently authenticated BUYER.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews for the user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ReviewDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER)")
    })
    @GetMapping("/user/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<ReviewDto>> getMyReviews() {
        return ResponseEntity.ok(reviewService.getReviewsByAuthenticatedUser());
    }

    @Operation(summary = "Get all reviews for a specific product (Public)",
            description = "Retrieves a list of all reviews submitted for a given product ID. This endpoint is public.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews for the product",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ReviewDto.class)))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(example = "{\"message\":\"Product not found with ID: 123\"}")))
    })
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByProduct(
            @Parameter(description = "ID of the product to fetch reviews for", required = true, example = "1")
            @PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @Operation(summary = "Add a new review for a product (Buyer only)",
            description = "Allows an authenticated BUYER to add a review for a product they have purchased. The user ID in the path must match the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review added successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (e.g., rating out of bounds, empty comment if required)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a BUYER, user ID mismatch, or user has not purchased the product, or already reviewed)"),
            @ApiResponse(responseCode = "404", description = "Product or User not found"),
            @ApiResponse(responseCode = "409", description = "Conflict (e.g., user has already reviewed this product)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"You have already reviewed this product.\"}")))
    })
    @PostMapping("/product/{productId}/user/{userId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ReviewDto> addReview(
            @Parameter(description = "ID of the product being reviewed", required = true, example = "1") @PathVariable Long productId,
            @Parameter(description = "ID of the user submitting the review (must match authenticated user)", required = true, example = "user-uuid-123") @PathVariable String userId,
            @Valid @RequestBody ReviewDto reviewDto) {

        User authenticatedUser = getAuthenticatedUser();
        if (!authenticatedUser.getId().equals(userId)) {
            throw new AccessDeniedException("User can only add reviews for themselves.");
        }
        return ResponseEntity.ok(reviewService.addReview(productId, userId, reviewDto));
    }

    @Operation(summary = "Update an existing review (Buyer Owner or Admin only)",
            description = "Allows the BUYER who owns the review or an ADMIN to update an existing review. The review ID is used to identify the review.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input for update"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner of the review and not an ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<ReviewDto> updateReview(
            @Parameter(description = "ID of the review to update", required = true, example = "10") @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDto reviewDto) {

        User authenticatedUser = getAuthenticatedUser();
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        String userRole = authenticatedUser.getRole().toUpperCase();

        if ("BUYER".equals(userRole)) {
            if (existingReview.getUser() == null || !existingReview.getUser().getId().equals(authenticatedUser.getId())) {
                throw new AccessDeniedException("Buyers can only update their own reviews.");
            }
        }
        return ResponseEntity.ok(reviewService.updateReview(reviewId, reviewDto));
    }

    @Operation(summary = "Delete a review (Buyer Owner or Admin only)",
            description = "Allows the BUYER who owns the review or an ADMIN to delete an existing review by its ID.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not the owner of the review and not an ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "ID of the review to delete", required = true, example = "10") @PathVariable Long reviewId) {
        User authenticatedUser = getAuthenticatedUser();
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        String userRole = authenticatedUser.getRole().toUpperCase();

        if ("BUYER".equals(userRole)) {
            if (existingReview.getUser() == null || !existingReview.getUser().getId().equals(authenticatedUser.getId())) {
                throw new AccessDeniedException("Buyers can only delete their own reviews.");
            }
        }
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}