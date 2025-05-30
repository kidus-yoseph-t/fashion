package com.project.Fashion.controller;

import com.project.Fashion.model.Cart;
import com.project.Fashion.service.CartService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/api/cart")
@AllArgsConstructor
public class CartController {
    private final CartService cartService;


    //create
    @PostMapping
    public ResponseEntity<Cart> addCart(@RequestBody Cart cart){
        return ResponseEntity.ok(cartService.addCart(cart));
    }

    // retrieve

    @GetMapping("/{id}")
    public ResponseEntity<Cart> getCart(@PathVariable String id) {
        return ResponseEntity.ok(cartService.getCart(id));
    }

    // update
    @PutMapping("/{id}")
    public ResponseEntity<Cart> updateCart(@PathVariable String id, @RequestBody Cart cart) {
        return ResponseEntity.ok(cartService.updateCart(id, cart));
    }

    // patch
    @PatchMapping("/{id}")
    public ResponseEntity<Cart> patchCart(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(cartService.patchCart(id, updates));
    }

    // delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable String id) {
        cartService.deleteCart(id);
        return ResponseEntity.noContent().build();
    }
}
