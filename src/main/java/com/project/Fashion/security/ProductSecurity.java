package com.project.Fashion.security;

import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository; // To fetch User by email
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Component("productSecurity") // Bean name for SpEL expression
public class ProductSecurity {

    private static final Logger logger = LoggerFactory.getLogger(ProductSecurity.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository; // Inject UserRepository

    /**
     * Checks if the authenticated user is the owner of the specified product.
     * This method is typically used in @PreAuthorize annotations for SELLER roles.
     *
     * @param authentication The current authentication object.
     * @param productId      The ID of the product to check.
     * @return true if the authenticated user is the seller of the product, false otherwise.
     */
    public boolean isOwner(Authentication authentication, Long productId) {
        // 1. Basic Authentication Checks
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("isOwner check failed: Authentication is null or not authenticated.");
            return false;
        }

        Object principal = authentication.getPrincipal();
        String authenticatedUserEmail;

        if (principal instanceof UserDetails) {
            authenticatedUserEmail = ((UserDetails) principal).getUsername(); // This is the email
        } else if (principal instanceof String) {
            authenticatedUserEmail = (String) principal; // Fallback if principal is just the username string
        } else {
            logger.warn("isOwner check failed: Principal is not an instance of UserDetails or String. Principal type: {}", principal.getClass().getName());
            return false;
        }

        if (authenticatedUserEmail == null || authenticatedUserEmail.isEmpty()) {
            logger.warn("isOwner check failed: Authenticated user email is null or empty.");
            return false;
        }

        // 2. Fetch Authenticated User's ID
        Optional<User> authenticatedUserOptional = userRepository.findByEmail(authenticatedUserEmail);
        if (authenticatedUserOptional.isEmpty()) {
            logger.warn("isOwner check failed: No user found with email: {}", authenticatedUserEmail);
            return false;
        }
        String authenticatedUserId = authenticatedUserOptional.get().getId();

        // 3. Fetch the Product
        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            logger.warn("isOwner check failed: Product not found with ID: {}", productId);
            // Or throw ProductNotFoundException if appropriate, though for a boolean check, returning false is typical.
            return false;
        }
        Product product = productOptional.get();

        // 4. Check Ownership
        if (product.getSeller() == null || product.getSeller().getId() == null) {
            logger.warn("isOwner check failed: Product with ID {} does not have a seller or seller ID is null.", productId);
            return false;
        }

        boolean isOwner = product.getSeller().getId().equals(authenticatedUserId);
        if (!isOwner) {
            logger.warn("Ownership check failed for product ID {}: Authenticated user ID {} (email: {}) is not the seller (seller ID: {}).",
                    productId, authenticatedUserId, authenticatedUserEmail, product.getSeller().getId());
        } else {
            logger.info("Ownership check successful for product ID {}: Authenticated user ID {} (email: {}) is the seller.",
                    productId, authenticatedUserId, authenticatedUserEmail);
        }
        return isOwner;
    }

    /**
     * Checks if the authenticated user is an Admin.
     * @param authentication The current authentication object.
     * @return true if the authenticated user has the ROLE_ADMIN authority.
     */
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }
}
