package com.project.Fashion.service;

import com.project.Fashion.dto.ProductDto; // Assuming you want to return ProductDto for favorites
import com.project.Fashion.config.mappers.ProductMapper; // To map Product to ProductDto
import com.project.Fashion.exception.exceptions.ProductNotFoundException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.model.UserFavorite;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserFavoriteRepository;
import com.project.Fashion.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteService.class);

    private final UserFavoriteRepository userFavoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper; // To convert Product entities to ProductDtos

    @Autowired
    public FavoriteService(UserFavoriteRepository userFavoriteRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           ProductMapper productMapper) {
        this.userFavoriteRepository = userFavoriteRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public void addFavorite(String userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        // Check if already favorited to prevent duplicate entries (unique constraint also handles this at DB level)
        if (userFavoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            logger.info("Product {} is already favorited by user {}", productId, userId);
            // Depending on desired behavior, you could throw an exception or just do nothing.
            // For idempotency, doing nothing if it already exists is fine.
            return;
        }

        UserFavorite userFavorite = new UserFavorite(user, product);
        userFavoriteRepository.save(userFavorite);
        logger.info("User {} added product {} to favorites.", userId, productId);
    }

    @Transactional
    public void removeFavorite(String userId, Long productId) {
        // Ensure user and product exist before attempting to delete the favorite relationship
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException("Product not found with ID: " + productId);
        }

        // Check if the favorite entry exists before trying to delete
        if (!userFavoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            logger.warn("Attempted to remove favorite for product {} by user {}, but it was not favorited.", productId, userId);
            // Depending on desired behavior, you could throw an exception or just log and return.
            // For idempotency of delete, if it doesn't exist, the state is already as desired.
            return;
        }

        userFavoriteRepository.deleteByUserIdAndProductId(userId, productId);
        logger.info("User {} removed product {} from favorites.", userId, productId);
    }

    @Transactional(readOnly = true) // readOnly for query methods
    public List<ProductDto> getFavoriteProductsByUserId(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        List<UserFavorite> favorites = userFavoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(UserFavorite::getProduct) // Get the Product entity from each UserFavorite
                .map(productMapper::toDto)     // Convert Product entity to ProductDto
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isProductFavoritedByUser(String userId, Long productId) {
        // Basic existence checks can be helpful but might be redundant if existsBy... handles it.
        // if (!userRepository.existsById(userId)) return false; // Or throw UserNotFoundException
        // if (!productRepository.existsById(productId)) return false; // Or throw ProductNotFoundException
        return userFavoriteRepository.existsByUserIdAndProductId(userId, productId);
    }
}
