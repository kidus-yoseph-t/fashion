package com.project.Fashion.repository;

import com.project.Fashion.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Finds products by seller ID with pagination and sorting.
     *
     * @param sellerId The ID of the seller.
     * @param pageable Pagination and sorting information.
     * @return A Page of products for the given seller.
     */
    Page<Product> findBySellerId(String sellerId, Pageable pageable);

    @Query("SELECT MIN(p.price) FROM Product p")
    Float findMinPrice();

    @Query("SELECT MAX(p.price) FROM Product p")
    Float findMaxPrice();
}
