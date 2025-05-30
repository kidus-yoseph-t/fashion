package com.project.Fashion.controller;

import com.project.Fashion.dto.CartRequestDto;
import com.project.Fashion.dto.CartResponseDto;
import com.project.Fashion.model.Cart;
import com.project.Fashion.service.CartService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/cart", produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CartResponseDto> addCart(@RequestBody CartRequestDto cartDto) {
        return ResponseEntity.ok(cartService.addCart(cartDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartResponseDto> getCart(@PathVariable String id) {
        Cart cart = cartService.getCart(id);
        return ResponseEntity.ok(cartService.convertToDTO(cart));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cart> updateCart(@PathVariable String id, @RequestBody Cart cart) {
        return ResponseEntity.ok(cartService.updateCart(id, cart));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Cart> patchCart(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(cartService.patchCart(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable String id) {
        cartService.deleteCart(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartResponseDto>> getCartsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCartDtosByUser(userId));
    }
}
