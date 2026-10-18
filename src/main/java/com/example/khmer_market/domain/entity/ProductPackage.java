package com.example.khmer_market.domain.entity;

import com.example.khmer_market.domain.enums.PackageStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_packages")
public class ProductPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "package_name", length = 100, nullable = false)
    private String packageName;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "unit_size", precision = 10, scale = 2)
    private BigDecimal unitSize;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "sku", length = 60, unique = true)
    private String sku;

    @Column(name = "status", length = 30, nullable = false)
    private PackageStatus status = PackageStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "productPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Inventory inventory;

    public ProductPackage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getUnitSize() { return unitSize; }
    public void setUnitSize(BigDecimal unitSize) { this.unitSize = unitSize; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) {
        this.price = price != null ? price.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public PackageStatus getStatus() { return status; }
    public void setStatus(PackageStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
        if (inventory != null) {
            inventory.setProductPackage(this);
        }
    }

    public boolean isActive() {
        return this.status == PackageStatus.ACTIVE;
    }
}
