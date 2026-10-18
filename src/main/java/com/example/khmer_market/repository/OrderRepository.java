package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"buyer", "address", "items"})
    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"buyer", "address", "items"})
    Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"buyer", "address"})
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"buyer", "address"})
    @Query("SELECT o FROM Order o WHERE o.buyer.id = :buyerId AND (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> findByBuyerIdAndStatus(@Param("buyerId") Long buyerId, @Param("status") OrderStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"buyer", "address"})
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items item WHERE item.seller.id = :sellerId " +
           "AND (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> findBySellerId(@Param("sellerId") Long sellerId,
                               @Param("status") OrderStatus status,
                               Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"buyer", "address"})
    @Query("SELECT o FROM Order o WHERE " +
           "(:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(o.buyer.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(o.buyer.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR o.status = :status)")
    Page<Order> searchAdminOrders(@Param("query") String query,
                                  @Param("status") OrderStatus status,
                                  Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.payments p WHERE o.status = 'pending' AND p.paymentStatus = 'pending' " +
           "AND p.paymentMethod = 'khqr' AND o.createdAt < :cutoff")
    List<Order> findUnpaidOnlineOrdersBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'paid'")
    BigDecimal sumTotalPaidRevenue();

    @Query("SELECT COALESCE(SUM(o.subtotal), 0) FROM Order o JOIN o.payments p WHERE p.paymentStatus = 'paid'")
    BigDecimal sumPaidOrdersSubtotal();

    @Query("SELECT COALESCE(SUM(o.shippingFee), 0) FROM Order o JOIN o.payments p WHERE p.paymentStatus = 'paid'")
    BigDecimal sumPaidOrdersShipping();

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("SELECT DATE(p.paymentDate), SUM(p.amount) FROM Payment p WHERE p.paymentStatus = 'paid' " +
           "AND p.paymentDate >= :since GROUP BY DATE(p.paymentDate) ORDER BY DATE(p.paymentDate) ASC")
    List<Object[]> findDailyRevenueSince(@Param("since") LocalDateTime since);

    @Query("SELECT item.seller.id, fp.farmName, SUM(item.subtotal) FROM OrderItem item " +
           "JOIN item.order o JOIN item.seller s JOIN s.farmerProfile fp " +
           "JOIN o.payments p WHERE p.paymentStatus = 'paid' " +
           "GROUP BY item.seller.id, fp.farmName ORDER BY SUM(item.subtotal) DESC")
    List<Object[]> findTopFarmersByRevenue(Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startOfDay")
    long countOrdersSince(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT p.category.name, SUM(item.subtotal) FROM OrderItem item " +
           "JOIN item.order o JOIN item.productPackage pp JOIN pp.product p " +
           "WHERE o.status IN ('confirmed', 'shipped', 'delivered') " +
           "GROUP BY p.category.name ORDER BY SUM(item.subtotal) DESC")
    List<Object[]> findTopCategoriesBySales(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.escrowStatus = 'HELD' AND o.escrowReleaseAt <= :now AND o.status != 'cancelled'")
    List<Order> findHeldEscrowsReadyForRelease(@Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.escrowStatus = 'HELD' AND o.status != 'cancelled'")
    BigDecimal sumHeldEscrowAmount();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.escrowStatus = 'RELEASED'")
    BigDecimal sumReleasedEscrowAmount();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"buyer", "address", "items"})
    @Query("SELECT o FROM Order o WHERE o.escrowStatus IS NOT NULL ORDER BY o.createdAt DESC")
    Page<Order> findEscrowOrders(Pageable pageable);
}
