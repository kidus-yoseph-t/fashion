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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName(); // Email
        return userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
    }

    private ReviewDto toDto(Review review) {
        if (review == null) return null;
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        if (review.getProduct() != null) {
            dto.setProductId(review.getProduct().getId());
            dto.setProductName(review.getProduct().getName()); // Populate the product name
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
            dto.setDate(DATE_FORMATTER.format(review.getDate().toInstant()));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByAuthenticatedUser() {
        User user = getAuthenticatedUser();
        logger.info("Fetching all reviews for authenticated user: {}", user.getEmail());
        List<Review> reviews = reviewRepository.findByUserId(user.getId());
        return reviews.stream().map(this::toDto).collect(Collectors.toList());
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
        List<Order> qualifyingOrders = orderRepository.findOrdersByUserProductAndStatuses(userId, productId, PURCHASED_ORDER_STATUSES);
        if (qualifyingOrders.isEmpty()) {
            logger.warn("User {} attempted to review product {} without a qualifying purchase.", userId, productId);
            throw new IllegalStateException("You can only review products you have purchased and received/paid for.");
        }
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            logger.warn("User {} attempted to review product {} again.", userId, productId);
            throw new IllegalStateException("You have already reviewed this product.");
        }
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setDate(new Date());
        Review savedReview = reviewRepository.save(review);
        logger.info("Review ID {} added by user {} for product {}", savedReview.getId(), userId, productId);
        updateProductRatingStats(productId);
        return toDto(savedReview);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));
        Long productId = review.getProduct().getId();
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
        Review updatedReview = reviewRepository.save(review);
        logger.info("Review ID {} updated for product {}", reviewId, review.getProduct().getId());
        updateProductRatingStats(review.getProduct().getId());
        return toDto(updatedReview);
    }

    private void updateProductRatingStats(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
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