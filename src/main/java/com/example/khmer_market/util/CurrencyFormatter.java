package com.example.khmer_market.util;

import com.example.khmer_market.service.StoreConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class CurrencyFormatter {

    @Value("${market.business.exchange-rate-usd-khr:4100}")
    private BigDecimal defaultExchangeRate = new BigDecimal("4100");

    private StoreConfigService storeConfigService;

    private static final DecimalFormat USD_FORMAT = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat KHR_FORMAT = new DecimalFormat("#,##0 ៛", new DecimalFormatSymbols(Locale.US));

    public CurrencyFormatter() {}

    @Autowired(required = false)
    public void setStoreConfigService(StoreConfigService storeConfigService) {
        this.storeConfigService = storeConfigService;
    }

    public String formatUsd(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return USD_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public String formatKhr(BigDecimal usdAmount) {
        if (usdAmount == null) {
            return "0 ៛";
        }
        BigDecimal rate = (storeConfigService != null) ? storeConfigService.getExchangeRateUsdKhr() : defaultExchangeRate;
        BigDecimal khrAmount = usdAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        return KHR_FORMAT.format(khrAmount);
    }

    public String formatDual(BigDecimal usdAmount) {
        if (usdAmount == null) {
            return "$0.00 (0 ៛)";
        }
        return formatUsd(usdAmount) + " (~" + formatKhr(usdAmount) + ")";
    }

    public BigDecimal getExchangeRateUsdKhr() {
        return (storeConfigService != null) ? storeConfigService.getExchangeRateUsdKhr() : defaultExchangeRate;
    }
}
