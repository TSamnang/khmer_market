package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductPackageId(Long cartId, Long packageId);

    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);

    long countByCartId(Long cartId);

    @Query("SELECT COALESCE(SUM(ci.quantity), 0L) FROM CartItem ci WHERE ci.cart.user.id = :userId")
    Long sumQuantityByUserId(@Param("userId") Long userId);
}
