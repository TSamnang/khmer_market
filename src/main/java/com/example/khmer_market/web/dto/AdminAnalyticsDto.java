package com.example.khmer_market.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminAnalyticsDto(
        List<String> revenueLabels,
        List<BigDecimal> revenueData,
        List<String> orderStatusLabels,
        List<Long> orderStatusData,
        List<String> roleLabels,
        List<Long> roleData,
        List<String> categoryLabels,
        List<BigDecimal> categoryData,
        List<String> topFarmerLabels,
        List<BigDecimal> topFarmerData
) {}
