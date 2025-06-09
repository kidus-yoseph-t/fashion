package com.project.Fashion.repository;

import com.project.Fashion.dto.CartResponseDto;
import com.project.Fashion.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByProductId(Long productId);
    List<Cart> findByUserId(String id);
    @Query("SELECT new com.project.Fashion.dto.CartResponseDto(" +
            "c.id, c.quantity, " +
            "p.id, p.name, p.photoUrl, p.category, p.price, " +
            "u.id) " +
            "FROM Cart c " +
            "JOIN c.product p " +
            "JOIN c.user u " +
            "WHERE u.id = :userId")
    List<CartResponseDto> findCartDtosByUserId(@Param("userId") String userId);
    Optional<Cart> findByUserIdAndProductId(String userId, Long productId);

}