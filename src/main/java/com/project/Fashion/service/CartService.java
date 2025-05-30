package com.project.Fashion.service;

import com.project.Fashion.dto.CartRequestDto;
import com.project.Fashion.dto.CartResponseDto;
import com.project.Fashion.model.Cart;
import com.project.Fashion.model.Product;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.CartRepository;
import com.project.Fashion.repository.ProductRepository;
import com.project.Fashion.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartResponseDto addCart(CartRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(requestDto.getQuantity());

        return convertToDTO(cartRepository.save(cart));
    }

    public Cart getCart(String id) {
        Long cartId = Long.parseLong(id);
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found with id: " + id));
    }

    public Cart updateCart(String id, Cart updatedCart) {
        Long cartId = Long.parseLong(id);
        Cart existingCart = getCart(id);

        existingCart.setProduct(updatedCart.getProduct());
        existingCart.setQuantity(updatedCart.getQuantity());

        return cartRepository.save(existingCart);
    }

    public Cart patchCart(String id, Map<String, Object> updates) {
        Cart cart = getCart(id);

        updates.forEach((key, value) -> {
            switch (key) {
                case "quantity" -> cart.setQuantity(Integer.parseInt(value.toString()));
                default -> throw new IllegalArgumentException("Invalid field: " + key);
            }
        });

        return cartRepository.save(cart);
    }

    public void deleteCart(String id) {
        Long cartId = Long.parseLong(id);
        if (!cartRepository.existsById(cartId)) {
            throw new RuntimeException("Cart not found with id: " + id);
        }
        cartRepository.deleteById(cartId);
    }

    public List<CartResponseDto> getCartDtosByUser(String userId) {
        return cartRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CartResponseDto convertToDTO(Cart cart) {
        CartResponseDto dto = new CartResponseDto();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());

        dto.setProductId(cart.getProduct().getId());
        dto.setProductName(cart.getProduct().getName());
        dto.setCategory(cart.getProduct().getCategory());
        dto.setPrice(cart.getProduct().getPrice());
        dto.setPhotoUrl(cart.getProduct().getPhotoUrl());

        dto.setQuantity(cart.getQuantity());

        return dto;
    }
}
