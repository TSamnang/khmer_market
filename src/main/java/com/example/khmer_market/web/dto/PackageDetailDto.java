package com.example.khmer_market.web.dto;

import java.math.BigDecimal;

public class PackageDetailDto {

    private Long id;
    private String packageName;
    private String unit;
    private BigDecimal unitSize;
    private BigDecimal price;
    private String sku;
    private int availableQuantity;
    private boolean inStock;

    public PackageDetailDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getUnitSize() { return unitSize; }
    public void setUnitSize(BigDecimal unitSize) { this.unitSize = unitSize; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    public int getAvailableStock() { return availableQuantity; }
    public BigDecimal getNetWeight() { return unitSize; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
}
