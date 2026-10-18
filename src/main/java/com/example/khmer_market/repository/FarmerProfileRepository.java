package com.example.khmer_market.repository;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {

    @Query("SELECT fp FROM FarmerProfile fp JOIN FETCH fp.user WHERE fp.user.id = :userId")
    Optional<FarmerProfile> findByUserId(@Param("userId") Long userId);

    @Query("SELECT fp FROM FarmerProfile fp JOIN FETCH fp.user WHERE fp.id = :id")
    Optional<FarmerProfile> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT DISTINCT fp.province FROM FarmerProfile fp WHERE fp.province IS NOT NULL AND fp.province != '' ORDER BY fp.province")
    List<String> findDistinctProvinces();

    @Query(value = "SELECT fp FROM FarmerProfile fp JOIN FETCH fp.user u WHERE " +
           "(:query IS NULL OR LOWER(fp.farmName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(fp.farmerName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(fp.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(fp.province) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:province IS NULL OR fp.province = :province) AND " +
           "(:status IS NULL OR u.status = :status)",
           countQuery = "SELECT COUNT(fp) FROM FarmerProfile fp JOIN fp.user u WHERE " +
           "(:query IS NULL OR LOWER(fp.farmName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(fp.farmerName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(fp.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(fp.province) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:province IS NULL OR fp.province = :province) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<FarmerProfile> searchFarmerProfiles(@Param("query") String query,
                                             @Param("province") String province,
                                             @Param("status") UserStatus status,
                                             Pageable pageable);

    @Query("SELECT COUNT(fp) FROM FarmerProfile fp JOIN fp.user u WHERE u.status = com.example.khmer_market.domain.enums.UserStatus.PENDING")
    long countPendingApprovals();
}
