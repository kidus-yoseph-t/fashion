package com.project.Fashion.repository;

import com.project.Fashion.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByProductId(Long productId);
    List<Cart> findByUserId(String id);
}

