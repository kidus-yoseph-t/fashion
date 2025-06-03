package com.project.Fashion.controller;

import com.project.Fashion.dto.ReviewDto;
import com.project.Fashion.model.Review;
import com.project.Fashion.model.User;
import com.project.Fashion.service.ReviewService;
import com.project.Fashion.repository.ReviewRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.ReviewNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository; // For fetching review directly for auth checks

    @Autowired
    public ReviewController(ReviewService reviewService,
                            UserRepository userRepository,
                            ReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Helper method to get the authenticated user's details.
     * @return Authenticated User object.
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName(); // Email
        return userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
    }

    // This endpoint is public as per SecurityConfig (GET /api/reviews/product/**). No specific auth check needed here.
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @PostMapping("/product/{productId}/user/{userId}")
    public ResponseEntity<ReviewDto> addReview(
            @PathVariable Long productId,
            @PathVariable String userId, // The user ID of the person submitting the review
            @RequestBody ReviewDto reviewDto) {

        User authenticatedUser = getAuthenticatedUser();

        // Authorization: Ensure the userId in the path matches the authenticated user.
        // Role check (BUYER) is handled by SecurityConfig.
        if (!authenticatedUser.getId().equals(userId)) {
            throw new AccessDeniedException("User can only add reviews for themselves.");
        }

        // The reviewDto might or might not contain userId and productId.
        // The service method addReview(productId, userId, reviewDto) uses the path variables, which is good.
        return ResponseEntity.ok(reviewService.addReview(productId, userId, reviewDto));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewDto reviewDto) {

        User authenticatedUser = getAuthenticatedUser();
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        String userRole = authenticatedUser.getRole().toUpperCase();

        if ("BUYER".equals(userRole)) {
            // Buyer can only update their own review.
            if (existingReview.getUser() == null || !existingReview.getUser().getId().equals(authenticatedUser.getId())) {
                throw new AccessDeniedException("Buyers can only update their own reviews.");
            }
        } else if (!"ADMIN".equals(userRole)) {
            // If not ADMIN or authorized BUYER
            throw new AccessDeniedException("User does not have permission to update this review.");
        }
        // ADMIN can update any review (role check from SecurityConfig is primary, this allows them through)

        // The reviewDto should contain the updated rating and comment.
        // The service method updateReview(reviewId, reviewDto) handles the update logic.
        return ResponseEntity.ok(reviewService.updateReview(reviewId, reviewDto));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        User authenticatedUser = getAuthenticatedUser();
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        String userRole = authenticatedUser.getRole().toUpperCase();

        if ("BUYER".equals(userRole)) {
            // Buyer can only delete their own review.
            if (existingReview.getUser() == null || !existingReview.getUser().getId().equals(authenticatedUser.getId())) {
                throw new AccessDeniedException("Buyers can only delete their own reviews.");
            }
        } else if (!"ADMIN".equals(userRole)) {
            // If not ADMIN or authorized BUYER
            throw new AccessDeniedException("User does not have permission to delete this review.");
        }
        // ADMIN can delete any review.

        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
