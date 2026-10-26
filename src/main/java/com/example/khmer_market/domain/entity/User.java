package com.example.khmer_market.domain.entity;

import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 256, nullable = false)
    private String password;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "role", length = 30, nullable = false)
    private UserRole role = UserRole.BUYER;

    @Column(name = "status", length = 30, nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "balance", precision = 12, scale = 2, nullable = false)
    private java.math.BigDecimal balance = java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private FarmerProfile farmerProfile;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private UserSecurity userSecurity;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPaymentMethod> paymentMethods = new ArrayList<>();

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public FarmerProfile getFarmerProfile() { return farmerProfile; }
    public void setFarmerProfile(FarmerProfile farmerProfile) {
        this.farmerProfile = farmerProfile;
        if (farmerProfile != null) {
            farmerProfile.setUser(this);
        }
    }

    public UserSecurity getUserSecurity() { return userSecurity; }
    public void setUserSecurity(UserSecurity userSecurity) {
        this.userSecurity = userSecurity;
        if (userSecurity != null) {
            userSecurity.setUser(this);
        }
    }

    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }

    public List<UserPaymentMethod> getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(List<UserPaymentMethod> paymentMethods) { this.paymentMethods = paymentMethods; }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isFarmer() {
        return this.role == UserRole.FARMER;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isAccountLocked() {
        return userSecurity != null && userSecurity.isAccountLocked();
    }

    public int getFailedAttempts() {
        return userSecurity != null ? userSecurity.getFailedAttempts() : 0;
    }

    public LocalDateTime getLockedUntil() {
        return userSecurity != null ? userSecurity.getLockedUntil() : null;
    }

    public java.math.BigDecimal getBalance() {
        return balance != null ? balance : java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void setBalance(java.math.BigDecimal balance) {
        this.balance = balance != null ? balance.setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void creditBalance(java.math.BigDecimal amount) {
        if (amount != null && amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            this.setBalance(this.getBalance().add(amount));
        }
    }

    public void debitBalance(java.math.BigDecimal amount) {
        if (amount != null && amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            this.setBalance(this.getBalance().subtract(amount));
        }
    }
}
