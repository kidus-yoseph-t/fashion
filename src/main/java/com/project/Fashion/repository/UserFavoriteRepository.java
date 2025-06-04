package com.project.Fashion.repository;

import com.project.Fashion.model.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    // Find all favorites for a given user ID
    List<UserFavorite> findByUserId(String userId);

    // Find a specific favorite entry by user ID and product ID
    Optional<UserFavorite> findByUserIdAndProductId(String userId, Long productId);

    // Check if a specific product is favorited by a specific user
    boolean existsByUserIdAndProductId(String userId, Long productId);

    // Delete a favorite entry by user ID and product ID (useful for unfavoriting)
    // Spring Data JPA can derive delete queries. Ensure it's transactional in the service layer.
    void deleteByUserIdAndProductId(String userId, Long productId);

    // You might also want to fetch favorites with product details eagerly if needed,
    // though often just getting the IDs and then fetching products separately is fine.
    // Example (more advanced, might require careful DTO projection or EntityGraph):
    // @Query("SELECT uf FROM UserFavorite uf JOIN FETCH uf.product WHERE uf.user.id = :userId")
    // List<UserFavorite> findByUserIdWithProductDetails(@Param("userId") String userId);
}
