package com.project.Fashion.repository;

import com.project.Fashion.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.stereotype.Repository; // Import Repository if not already there

import java.util.List;

@Repository // Ensure @Repository is present
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findBySellerId(String sellerId);

    // New method to get distinct category names
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category ASC")
    List<String> findDistinctCategories();

    // New method to get overall min and max price
    // We'll create a DTO for this later, for now, let's conceptualize.
    // This could be two methods or one returning a custom object/array.
    @Query("SELECT MIN(p.price) FROM Product p")
    Float findMinPrice();

    @Query("SELECT MAX(p.price) FROM Product p")
    Float findMaxPrice();
}
