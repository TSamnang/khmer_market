package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findBySellerId(Long sellerId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id AND oi.order.buyer.id = :buyerId")
    Optional<OrderItem> findByIdAndBuyerId(@Param("id") Long id, @Param("buyerId") Long buyerId);

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.productPackage.id = :packageId")
    long countByProductPackageId(@Param("packageId") Long packageId);

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.productPackage.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);
}
