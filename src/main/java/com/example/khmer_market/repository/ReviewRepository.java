package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.Review;
import com.example.khmer_market.domain.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status, Pageable pageable);

    Optional<Review> findByOrderItemId(Long orderItemId);

    boolean existsByOrderItemId(Long orderItemId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :productId AND r.status = 'published'")
    Double getAverageRatingForProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = 'published'")
    long countPublishedReviewsForProduct(@Param("productId") Long productId);

    @Query("SELECT r FROM Review r WHERE " +
           "(:query IS NULL OR LOWER(r.comment) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.product.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.user.username) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:rating IS NULL OR r.rating = :rating) AND " +
           "(:status IS NULL OR r.status = :status)")
    Page<Review> searchAdminReviews(@Param("query") String query,
                                    @Param("rating") Integer rating,
                                    @Param("status") ReviewStatus status,
                                    Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
