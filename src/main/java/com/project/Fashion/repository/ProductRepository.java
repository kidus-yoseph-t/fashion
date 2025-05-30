package com.project.Fashion.repository;

import com.project.Fashion.model.Product;
import org.springframework.data.domain.Page; // Keep this if other specific pageable finders are added later
import org.springframework.data.domain.Pageable; // Keep this for the same reason
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findBySellerId(String sellerId);
}
