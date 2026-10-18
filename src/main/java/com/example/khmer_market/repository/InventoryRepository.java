package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductPackageId(Long packageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT inv FROM Inventory inv WHERE inv.productPackage.id = :packageId")
    Optional<Inventory> findByPackageIdForUpdate(@Param("packageId") Long packageId);

    @Query("SELECT inv FROM Inventory inv " +
           "JOIN inv.productPackage pp " +
           "JOIN pp.product p " +
           "WHERE (inv.quantity - inv.reservedQuantity) <= inv.lowStockThreshold")
    List<Inventory> findLowStockInventories();

    @Query("SELECT inv FROM Inventory inv " +
           "JOIN inv.productPackage pp " +
           "JOIN pp.product p " +
           "WHERE p.seller.id = :sellerId AND (inv.quantity - inv.reservedQuantity) <= inv.lowStockThreshold")
    List<Inventory> findLowStockInventoriesForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(inv) FROM Inventory inv WHERE (inv.quantity - inv.reservedQuantity) <= inv.lowStockThreshold")
    long countLowStockAlerts();
}
