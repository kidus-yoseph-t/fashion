package com.project.Fashion.service;

import com.project.Fashion.dto.ReviewDto;
import com.project.Fashion.exception.exceptions.ProductNotFoundException;
import com.project.Fashion.exception.exceptions.ReviewNotFoundException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.Order;
import com.project.Fashion.model.OrderStatus;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.Review;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.OrderRepository;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.ReviewRepository;
import com.project.Fashion.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
//import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    // Define statuses that indicate a product has been effectively purchased
    private static final List<OrderStatus> PURCHASED_ORDER_STATUSES = Arrays.asList(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.COMPLETED
    );

    @Autowired
    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository,
                         UserRepository userRepository,
                         OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

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
            dto.setUserName("Anonymous"); // Should ideally not happen if user is required
        }
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        if (review.getDate() != null) {
            // Convert legacy java.util.Date to modern java.time.Instant before formatting
            dto.setDate(DATE_FORMATTER.format(review.getDate().toInstant()));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByProduct(Long productId) {
        // Check if product exists before attempting to fetch reviews
        if (!productRepository.existsById(productId)) {
            logger.info("No product found with ID {} when fetching reviews. Returning empty list.", productId);
            // Optionally, throw ProductNotFoundException if strict behavior is desired
            // throw new ProductNotFoundException("Product not found with ID: " + productId);
            return List.of(); // Return empty list if product doesn't exist
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

        // Check if the user has purchased this product
        List<Order> qualifyingOrders = orderRepository.findOrdersByUserProductAndStatuses(userId, productId, PURCHASED_ORDER_STATUSES);
        if (qualifyingOrders.isEmpty()) {
            logger.warn("User {} attempted to review product {} without a qualifying purchase.", userId, productId);
            throw new IllegalStateException("You can only review products you have purchased and received/paid for.");
        }

        // Optional: Check if this user has already reviewed this product to prevent multiple reviews
        // You would need a method like `existsByProductIdAndUserId` in ReviewRepository
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            logger.warn("User {} attempted to review product {} again.", userId, productId);
            throw new IllegalStateException("You have already reviewed this product.");
        }


        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setDate(new Date()); // Set current date for the review

        Review savedReview = reviewRepository.save(review);
        logger.info("Review ID {} added by user {} for product {}", savedReview.getId(), userId, productId);

        // Update product's average rating and number of reviews
        updateProductRatingStats(productId);

        return toDto(savedReview);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));

        Long productId = review.getProduct().getId(); // Get productId before deleting the review

        reviewRepository.deleteById(reviewId);
        logger.info("Review ID {} deleted by user (or admin) for product {}", reviewId, productId);

        // Update product's average rating and number of reviews
        updateProductRatingStats(productId);
    }

    @Transactional
    public ReviewDto updateReview(Long reviewId, ReviewDto reviewDto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));

        // Update review fields
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setDate(new Date()); // Update date to reflect modification time

        Review updatedReview = reviewRepository.save(review);
        logger.info("Review ID {} updated for product {}", reviewId, review.getProduct().getId());

        // Update product's average rating and number of reviews
        updateProductRatingStats(review.getProduct().getId());

        return toDto(updatedReview);
    }

    /**
     * Updates the average rating and number of reviews for a given product.
     * This method is called after a review is added, updated, or deleted.
     *
     * @param productId The ID of the product whose rating stats need to be updated.
     */
    private void updateProductRatingStats(Long productId) {
        Product product = productRepository.findById(productId)
                .orElse(null); // Find the product, or return null if not found

        if (product == null) {
            // This case should ideally not happen if productId is always valid from an existing review's product
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
        productRepository.save(product); // Save the updated product information
        logger.info("Updated rating stats for product ID {}: AvgRating={}, NumReviews={}",
                productId, product.getAverageRating(), product.getNumOfReviews());
    }
}
