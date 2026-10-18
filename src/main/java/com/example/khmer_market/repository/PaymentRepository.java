package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.Payment;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    @Query("SELECT p FROM Payment p WHERE " +
           "(:query IS NULL OR LOWER(p.transactionId) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.order.orderNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:method IS NULL OR p.paymentMethod = :method) AND " +
           "(:status IS NULL OR p.paymentStatus = :status)")
    Page<Payment> searchAdminPayments(@Param("query") String query,
                                      @Param("method") PaymentMethod method,
                                      @Param("status") PaymentStatus status,
                                      Pageable pageable);

    @Query("SELECT p.paymentMethod, COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'paid' GROUP BY p.paymentMethod")
    List<Object[]> countPaymentsByMethod();
}
