package com.example.khmer_market.web.dto;

import java.math.BigDecimal;

public record AdminDashboardStatsDto(
        BigDecimal platformNetRevenue,
        BigDecimal marketplaceGmv,
        BigDecimal farmerDisbursedEarnings,
        BigDecimal commissionPercentage,
        BigDecimal withdrawalFee,
        long totalOrders,
        long activeFarmers,
        long pendingFarmerApprovals,
        long lowStockAlertCount,
        long todayOrders
) {
    // Backwards-compatible alias for existing templates/tests referencing totalRevenue
    public BigDecimal totalRevenue() {
        return platformNetRevenue;
    }

    public BigDecimal getTotalRevenue() {
        return platformNetRevenue;
    }

    public BigDecimal getPlatformNetRevenue() {
        return platformNetRevenue;
    }

    public BigDecimal getMarketplaceGmv() {
        return marketplaceGmv;
    }

    public BigDecimal getFarmerDisbursedEarnings() {
        return farmerDisbursedEarnings;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public BigDecimal getWithdrawalFee() {
        return withdrawalFee;
    }
}
