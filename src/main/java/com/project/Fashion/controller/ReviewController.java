package com.project.Fashion.controller;

import com.project.Fashion.dto.ReviewDto;
import com.project.Fashion.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @PostMapping("/product/{productId}/user/{userId}")
    public ResponseEntity<ReviewDto> addReview(
            @PathVariable Long productId,
            @PathVariable String userId,
            @RequestBody ReviewDto reviewDto) {
        return ResponseEntity.ok(reviewService.addReview(productId, userId, reviewDto));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewDto reviewDto) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, reviewDto));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

}
