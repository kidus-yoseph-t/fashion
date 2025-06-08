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

    /**
     * Finds orders containing products sold by a specific seller, with pagination.
     * Spring Data JPA will generate a query like:
     * SELECT o FROM Order o WHERE o.product.seller.id = :sellerId
     *
     * @param sellerId The ID of the seller.
     * @param pageable Pagination and sorting information.
     * @return A Page of orders.
     */
    Page<Order> findByProduct_Seller_Id(String sellerId, Pageable pageable);
}
