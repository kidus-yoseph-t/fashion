package com.project.Fashion.repository;

import com.project.Fashion.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);
    List<Review> findByUserId(String userId);

    /**
     * Checks if a review exists for a given product ID and user ID.
     * This can be used to prevent a user from submitting multiple reviews for the same product.
     *
     * @param productId The ID of the product.
     * @param userId The ID of the user.
     * @return true if a review exists, false otherwise.
     */
    boolean existsByProductIdAndUserId(Long productId, String userId);
}
