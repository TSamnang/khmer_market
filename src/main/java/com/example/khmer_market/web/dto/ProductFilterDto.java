package com.example.khmer_market.web.dto;

import java.math.BigDecimal;

public class ProductFilterDto {

    private String query;
    private Long categoryId;
    private String province;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStockOnly = false;
    private String sort = "newest"; // newest | price_asc | price_desc | rating
    private int page = 0;
    private int size = 12;

    public ProductFilterDto() {}

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Boolean getInStockOnly() { return inStockOnly; }
    public void setInStockOnly(Boolean inStockOnly) { this.inStockOnly = inStockOnly; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public int getPage() { return Math.max(0, page); }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return Math.min(50, Math.max(1, size)); }
    public void setSize(int size) { this.size = size; }
}
