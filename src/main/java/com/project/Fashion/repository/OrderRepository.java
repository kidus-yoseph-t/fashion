package com.project.Fashion.repository;

import com.project.Fashion.model.Order;
import com.project.Fashion.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
    Page<Order> findByUserId(String userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.product.id = :productId AND o.status IN :statuses")
    List<Order> findOrdersByUserProductAndStatuses(
            @Param("userId") String userId,
            @Param("productId") Long productId,
            @Param("statuses") List<OrderStatus> statuses
    );

    Page<Order> findByProduct_Seller_Id(String sellerId, Pageable pageable);

    /**
     * Efficiently checks if a user has at least one order for a specific product
     * with a qualifying status (e.g., PAID, COMPLETED, SHIPPED).
     * @param userId The ID of the user.
     * @param productId The ID of the product.
     * @param statuses A list of qualifying order statuses.
     * @return true if a matching order exists, false otherwise.
     */
    boolean existsByUser_IdAndProduct_IdAndStatusIn(String userId, Long productId, List<OrderStatus> statuses);

    /**
     * Finds all orders for a seller's products that match one of the given statuses.
     * @param sellerId The ID of the seller.
     * @param statuses A list of order statuses to filter by.
     * @return A list of matching orders.
     */
    List<Order> findByProduct_Seller_IdAndStatusIn(String sellerId, List<OrderStatus> statuses);
}