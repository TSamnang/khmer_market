package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.UserPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPaymentMethodRepository extends JpaRepository<UserPaymentMethod, Long> {

    List<UserPaymentMethod> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    List<UserPaymentMethod> findByUserId(Long userId);

    Optional<UserPaymentMethod> findByIdAndUserId(Long id, Long userId);

    Optional<UserPaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserPaymentMethod m SET m.isDefault = false WHERE m.user.id = :userId")
    void clearDefaultMethodsForUser(@Param("userId") Long userId);
}
