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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class CartService {
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

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(requestDto.getQuantity());

        Cart savedCart = cartRepository.save(cart);
        return convertToDTO(savedCart);
    }

    public Cart getCartById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + cartId));
    }

    public Cart updateCartItemQuantity(Long cartId, int newQuantity) {
        if (newQuantity <= 0) {
            throw new InvalidFieldException("Quantity must be positive.");
        }
        Cart cart = getCartById(cartId);
        cart.setQuantity(newQuantity);
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
                    throw new InvalidFieldException("Quantity must be positive.");
                }
                cart.setQuantity(quantity);
            } else {
                throw new InvalidFieldException("Field '" + key + "' cannot be updated via this patch method. Only 'quantity' is supported.");
            }
        }
        return cartRepository.save(cart);
    }

    public void deleteCart(Long cartId) {
        if (!cartRepository.existsById(cartId)) {
            throw new CartNotFoundException("Cart not found with id: " + cartId);
        }
        cartRepository.deleteById(cartId);
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
        dto.setId(cart.getId());

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
