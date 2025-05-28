package com.project.Fashion.repository;

import com.project.Fashion.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
    List<Product> findBySellerId(String sellerId);
    List<Product> findByAverageRating(float averageRating);
}

