package com.project.Fashion.service;

import com.project.Fashion.dto.CartRequestDto;
import com.project.Fashion.dto.CartResponseDto;
import com.project.Fashion.exception.exceptions.CartNotFoundException;
import com.project.Fashion.exception.exceptions.InvalidFieldException;
import com.project.Fashion.exception.exceptions.ProductNotFoundException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.Cart;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.CartRepository;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartResponseDto addCart(CartRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + requestDto.getUserId()));

        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + requestDto.getProductId()));

        if (requestDto.getQuantity() <= 0) {
            throw new InvalidFieldException("Quantity must be positive.");
        }

        // Check if the user already has this product in their cart
        Optional<Cart> existingCartItemOptional = cartRepository.findByUserIdAndProductId(user.getId(), product.getId());

        Cart cartItemToSave;
        if (existingCartItemOptional.isPresent()) {
            // Product already in cart, update quantity
            cartItemToSave = existingCartItemOptional.get();
            cartItemToSave.setQuantity(cartItemToSave.getQuantity() + requestDto.getQuantity());
            logger.info("Updated quantity for product {} in cart for user {}", product.getId(), user.getId());
        } else {
            // New product in cart, create new entry
            cartItemToSave = new Cart();
            cartItemToSave.setUser(user);
            cartItemToSave.setProduct(product);
            cartItemToSave.setQuantity(requestDto.getQuantity());
            logger.info("Added new product {} to cart for user {}", product.getId(), user.getId());
        }

        Cart savedCart = cartRepository.save(cartItemToSave);
        return convertToDTO(savedCart);
    }

    public Cart getCartById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + cartId));
    }

    public Cart updateCartItemQuantity(Long cartId, int newQuantity) {
        if (newQuantity <= 0) {
            // Frontend CartContext already handles quantity <= 0 by calling removeFromCart.
            // However, a direct API call might still send 0 or negative.
            // Depending on desired behavior, either throw error or treat as delete.
            // For consistency with frontend, let's assume if it reaches here, it's an error or needs deletion.
            // Or, the controller could call deleteCart if newQuantity <= 0.
            // For now, let's enforce positive quantity at service level too for this specific method.
            throw new InvalidFieldException("Quantity must be positive for an update. To remove, use delete.");
        }
        Cart cart = getCartById(cartId);
        cart.setQuantity(newQuantity);
        logger.info("Updated quantity to {} for cart item {}", newQuantity, cartId);
        return cartRepository.save(cart);
    }

    public Cart patchCart(Long cartId, Map<String, Object> updates) {
        Cart cart = getCartById(cartId);

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("quantity".equals(key)) {
                if (!(value instanceof Integer)) {
                    throw new InvalidFieldException("Quantity must be an integer value.");
                }
                int quantity = (Integer) value;
                if (quantity <= 0) {
                    // If quantity becomes 0 or less via patch, it should be deleted.
                    // This logic is better handled by ensuring controller calls deleteCart or
                    // this method calls deleteCart and returns null/throws.
                    // For now, we'll allow setting quantity and let controller logic decide on deletion if 0.
                    // Or, more simply, throw error here.
                    throw new InvalidFieldException("Quantity must be positive. To remove item, use delete endpoint.");
                }
                cart.setQuantity(quantity);
                logger.info("Patched quantity to {} for cart item {}", quantity, cartId);
            } else {
                throw new InvalidFieldException("Field '" + key + "' cannot be updated via this patch method. Only 'quantity' is supported.");
            }
        }
        return cartRepository.save(cart);
    }

    public void deleteCartItem(Long cartId) { // Renamed from deleteCart for clarity
        if (!cartRepository.existsById(cartId)) {
            // Optional: throw CartNotFoundException, but controller already fetches first
            logger.warn("Attempted to delete non-existent cart item with id: {}", cartId);
            return; // Or throw
        }
        cartRepository.deleteById(cartId);
        logger.info("Deleted cart item with id: {}", cartId);
    }

    @Transactional
    public void clearUserCart(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        List<Cart> userCartItems = cartRepository.findByUserId(user.getId());
        if (!userCartItems.isEmpty()) {
            cartRepository.deleteAll(userCartItems); // More efficient batch delete
            logger.info("Cleared all {} cart items for user {}", userCartItems.size(), userId);
        } else {
            logger.info("No cart items to clear for user {}", userId);
        }
    }

    public List<CartResponseDto> getCartDtosByUser(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        return cartRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CartResponseDto convertToDTO(Cart cart) {
        if (cart == null) return null;
        CartResponseDto dto = new CartResponseDto();
        dto.setId(cart.getId()); // This is cart_item_id

        if (cart.getUser() != null) {
            dto.setUserId(cart.getUser().getId());
        }

        if (cart.getProduct() != null) {
            dto.setProductId(cart.getProduct().getId());
            dto.setProductName(cart.getProduct().getName());
            dto.setCategory(cart.getProduct().getCategory());
            dto.setPrice(cart.getProduct().getPrice());
            dto.setPhotoUrl(cart.getProduct().getPhotoUrl());
        }

        dto.setQuantity(cart.getQuantity());
        return dto;
    }
}
