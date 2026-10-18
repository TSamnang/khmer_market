package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Address> findByUserId(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.address.id = :addressId")
    long countOrdersUsingAddress(@Param("addressId") Long addressId);
}
