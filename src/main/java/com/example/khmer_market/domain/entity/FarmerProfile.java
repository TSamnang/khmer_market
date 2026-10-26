package com.example.khmer_market.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "farmer_profiles")
public class FarmerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "farm_name", length = 150, nullable = false)
    private String farmName;

    @Column(name = "farmer_name", length = 150, nullable = false)
    private String farmerName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", length = 100)
    private String address;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "farm_description", columnDefinition = "TEXT")
    private String farmDescription;

    @Column(name = "farm_image", length = 255)
    private String farmImage;

    @Column(name = "dob")
    private java.time.LocalDate dob;

    @Column(name = "id_type", length = 50)
    private String idType;

    @Column(name = "id_number", length = 100)
    private String idNumber;

    @Column(name = "id_front_image", length = 255)
    private String idFrontImage;

    @Column(name = "id_back_image", length = 255)
    private String idBackImage;

    @Column(name = "verification_status", length = 30, nullable = false)
    private String verificationStatus = "PENDING";

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public FarmerProfile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getFarmDescription() { return farmDescription; }
    public void setFarmDescription(String farmDescription) { this.farmDescription = farmDescription; }

    public String getDescription() { return farmDescription; }
    public void setDescription(String description) { this.farmDescription = description; }

    public String getFarmImage() { return farmImage; }
    public void setFarmImage(String farmImage) { this.farmImage = farmImage; }

    public java.time.LocalDate getDob() { return dob; }
    public void setDob(java.time.LocalDate dob) { this.dob = dob; }

    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getIdFrontImage() { return idFrontImage; }
    public void setIdFrontImage(String idFrontImage) { this.idFrontImage = idFrontImage; }

    public String getIdBackImage() { return idBackImage; }
    public void setIdBackImage(String idBackImage) { this.idBackImage = idBackImage; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(this.verificationStatus);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.verificationStatus);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(this.verificationStatus);
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
