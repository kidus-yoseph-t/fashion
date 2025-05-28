package com.project.Fashion.service;

import com.project.Fashion.model.Cart;
import com.project.Fashion.repository.CartRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class CartService {
    private final CartRepository cartRepository;

    // Create a new cart item
    public Cart addCart(Cart cart) {
        return cartRepository.save(cart);
    }

    // Retrieve a cart by ID
    public Cart getCart(String id) {
        Long cartId = Long.parseLong(id);
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with id: " + id));
    }

    // Update a cart entirely
    public Cart updateCart(String id, Cart updatedCart) {
        Long cartId = Long.parseLong(id);
        Cart existingCart = getCart(id);

        existingCart.setProduct(updatedCart.getProduct());
        existingCart.setQuantity(updatedCart.getQuantity());

        return cartRepository.save(existingCart);
    }

    // Patch specific fields of the cart
    public Cart patchCart(String id, Map<String, Object> updates) {
        Long cartId = Long.parseLong(id);
        Cart cart = getCart(id);

        updates.forEach((key, value) -> {
            switch (key) {
                case "quantity" -> cart.setQuantity(Integer.parseInt(value.toString()));
                // Add more fields if needed
                default -> throw new IllegalArgumentException("Invalid field: " + key);
            }
        });

        return cartRepository.save(cart);
    }

    // Delete a cart by ID
    public void deleteCart(String id) {
        Long cartId = Long.parseLong(id);
        if (!cartRepository.existsById(cartId)) {
            throw new RuntimeException("Cart not found with id: " + id);
        }
        cartRepository.deleteById(cartId);
    }

    // Optional: retrieve all cart items for a product
    public List<Cart> getCartsByProductId(Long productId) {
        return cartRepository.findByProductId(productId);
    }
}
