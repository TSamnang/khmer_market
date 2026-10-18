package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.WalletTransaction;
import com.example.khmer_market.domain.enums.WalletTransactionStatus;
import com.example.khmer_market.domain.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<WalletTransaction> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.type IN :types AND t.status = :status")
    BigDecimal sumAmountByTypeInAndStatus(@Param("types") Collection<WalletTransactionType> types,
                                          @Param("status") WalletTransactionStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.type = :type AND t.status = :status")
    BigDecimal sumAmountByTypeAndStatus(@Param("type") WalletTransactionType type,
                                        @Param("status") WalletTransactionStatus status);
}
