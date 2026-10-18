package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.Product;
import com.example.khmer_market.domain.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    long countBySellerId(Long sellerId);

    long countByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId AND " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Product> searchFarmerProducts(@Param("sellerId") Long sellerId,
                                       @Param("query") String query,
                                       @Param("status") ProductStatus status,
                                       Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.seller.username) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:sellerId IS NULL OR p.seller.id = :sellerId) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Product> searchAdminProducts(@Param("query") String query,
                                      @Param("categoryId") Long categoryId,
                                      @Param("sellerId") Long sellerId,
                                      @Param("status") ProductStatus status,
                                      Pageable pageable);

    @Query("SELECT p FROM Product p " +
           "JOIN p.seller s " +
           "JOIN s.farmerProfile fp " +
           "WHERE p.status = 'active' AND s.status = 'active' AND " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:province IS NULL OR fp.province = :province) AND " +
           "(:farmerId IS NULL OR s.id = :farmerId)")
    Page<Product> findPublicCatalogProducts(@Param("query") String query,
                                            @Param("categoryId") Long categoryId,
                                            @Param("province") String province,
                                            @Param("farmerId") Long farmerId,
                                            Pageable pageable);

    @Query("SELECT p.category.name, COUNT(p) FROM Product p GROUP BY p.category.name")
    List<Object[]> countProductsByCategory();
}
