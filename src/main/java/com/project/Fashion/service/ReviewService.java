package com.project.Fashion.service;

import com.project.Fashion.dto.ReviewDto;
import com.project.Fashion.exception.exceptions.ProductNotFoundException;
import com.project.Fashion.exception.exceptions.ReviewNotFoundException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.Review;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.ReviewRepository;
import com.project.Fashion.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private ReviewDto toDto(Review review) {
        if (review == null) return null;
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        if (review.getProduct() != null) {
            dto.setProductId(review.getProduct().getId());
        }
        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
            dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        } else {
            dto.setUserName("Anonymous");
        }
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        if (review.getDate() != null) {
            dto.setDate(DATE_FORMAT.format(review.getDate()));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            logger.info("No product found with ID {} when fetching reviews. Returning empty list.", productId);
            return List.of();
        }
        return reviewRepository.findByProductId(productId)
                .stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDto addReview(Long productId, String userId, ReviewDto reviewDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId + ". Cannot add review."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId + ". Cannot add review."));

        // Optional: Check if this user has already reviewed this product
        // if (reviewRepository.existsByProductIdAndUserId(productId, userId)) { // You'd need to add this method to ReviewRepository
        //    logger.warn("User {} attempted to review product {} again.", userId, productId);
        //    throw new IllegalStateException("You have already reviewed this product.");
        // }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setDate(new Date());

        Review saved = reviewRepository.save(review);
        logger.info("Review ID {} added by user {} for product {}", saved.getId(), userId, productId);
        updateProductRatingStats(productId);
        return toDto(saved);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));

        Long productId = review.getProduct().getId();
        String userId = review.getUser().getId();

        reviewRepository.deleteById(reviewId);
        logger.info("Review ID {} deleted by user (or admin) for product {}", reviewId, productId);
        updateProductRatingStats(productId);
    }

    @Transactional
    public ReviewDto updateReview(Long reviewId, ReviewDto reviewDto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));

        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setDate(new Date());

        Review updated = reviewRepository.save(review);
        logger.info("Review ID {} updated for product {}", reviewId, review.getProduct().getId());
        updateProductRatingStats(review.getProduct().getId());
        return toDto(updated);
    }

    private void updateProductRatingStats(Long productId) {
        Product product = productRepository.findById(productId)
                .orElse(null);
        if (product == null) {
            logger.warn("Product not found with ID {} during rating stats update. Cannot update product rating.", productId);
            return;
        }

        List<Review> reviewsForProduct = reviewRepository.findByProductId(productId);
        if (reviewsForProduct.isEmpty()) {
            product.setAverageRating(0.0f);
            product.setNumOfReviews(0);
        } else {
            double sumRatings = reviewsForProduct.stream().mapToDouble(Review::getRating).sum();
            product.setAverageRating((float) (sumRatings / reviewsForProduct.size()));
            product.setNumOfReviews(reviewsForProduct.size());
        }
        productRepository.save(product);
        logger.info("Updated rating stats for product ID {}: AvgRating={}, NumReviews={}",
                productId, product.getAverageRating(), product.getNumOfReviews());
    }
}