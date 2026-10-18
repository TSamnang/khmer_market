package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.ProductPackage;
import com.example.khmer_market.domain.enums.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPackageRepository extends JpaRepository<ProductPackage, Long> {

    List<ProductPackage> findByProductId(Long productId);

    List<ProductPackage> findByProductIdAndStatus(Long productId, PackageStatus status);

    Optional<ProductPackage> findByIdAndProductId(Long id, Long productId);

    Optional<ProductPackage> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("SELECT MIN(pp.price) FROM ProductPackage pp WHERE pp.product.id = :productId AND pp.status = 'active'")
    BigDecimal findMinActivePriceByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(pp) > 0 FROM ProductPackage pp " +
           "JOIN pp.inventory inv " +
           "WHERE pp.product.id = :productId AND pp.status = 'active' AND (inv.quantity - inv.reservedQuantity) > 0")
    boolean isProductInStock(@Param("productId") Long productId);
}
