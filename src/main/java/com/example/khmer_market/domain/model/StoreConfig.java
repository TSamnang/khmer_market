package com.example.khmer_market.domain.model;

import java.math.BigDecimal;

public class StoreConfig {

    // 1. Branding & Identity
    private String storeName = "Khmer Marketplace";
    private String tagline = "Direct Agricultural Marketplace from Cambodian Farmers";
    private String brandColor = "#047857";

    // 2. Announcement Banner
    private boolean announcementEnabled = true;
    private String announcementText = "🎉 Free direct delivery from Cambodian farms across the kingdom for orders over $25! Zero transaction fees with Bakong KHQR.";
    private String announcementBgClass = "bg-success"; // bg-success, bg-warning text-dark, bg-primary, bg-dark

    // 3. Hero Showcase
    private String heroBadgeText = "Direct from Cambodian Farmers to Your Table";
    private String heroTitle = "Fresh Agricultural Harvest Direct to Your Door";
    private String heroSubtitle = "Support local smallholders from Battambang, Kampot, Takeo, and Mondulkiri with direct farm-to-table delivery.";
    private String heroPrimaryBtnText = "Browse All Products";
    private String heroSecondaryBtnText = "Sell as a Farmer";

    // 4. Financial & Shipping Business Rules
    private BigDecimal exchangeRateUsdKhr = new BigDecimal("4100");
    private BigDecimal shippingFlatRate = new BigDecimal("1.50");
    private BigDecimal shippingFreeThreshold = new BigDecimal("25.00");
    private BigDecimal commissionPercentage = new BigDecimal("5.00"); // 5.00% platform commission on produce subtotal
    private BigDecimal withdrawalFee = new BigDecimal("0.25"); // $0.25 flat withdrawal processing fee

    // 5. Contact & Support Details
    private String supportEmail = "support@khmermarket.gov.kh";
    private String supportPhone = "+855 23 888 999";
    private String operatingHours = "Mon - Sat: 7:00 AM - 6:00 PM";
    private String officeAddress = "Sangkat Boeng Keng Kang 1, Khan Boeng Keng Kang, Phnom Penh, Kingdom of Cambodia";

    // 6. Platform Master Bank & Gateway Integration
    private String bankName = "ABA Bank";
    private String bankAccountNumber = "001 888 999";
    private String bankAccountName = "KHMER MARKET CO., LTD (ESCROW MASTER)";
    private String bankBranch = "Central Head Office, Phnom Penh";
    private String bakongMerchantId = "khmer_market_master@aba";
    private String bankApiKey = "pk_live_aba_99381748921";
    private String bankApiSecret = "sk_live_aba_sec_884920183749";
    private String bankWebhookSecret = "whsec_aba_928371928374";
    private String bankEnvironment = "LIVE"; // LIVE, SANDBOX
    private int escrowHoldDays = 7; // Auto-confirm in 7 days

    public StoreConfig() {}

    // Getters and Setters
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getBrandColor() { return brandColor; }
    public void setBrandColor(String brandColor) { this.brandColor = brandColor; }

    public boolean isAnnouncementEnabled() { return announcementEnabled; }
    public void setAnnouncementEnabled(boolean announcementEnabled) { this.announcementEnabled = announcementEnabled; }

    public String getAnnouncementText() { return announcementText; }
    public void setAnnouncementText(String announcementText) { this.announcementText = announcementText; }

    public String getAnnouncementBgClass() { return announcementBgClass; }
    public void setAnnouncementBgClass(String announcementBgClass) { this.announcementBgClass = announcementBgClass; }

    public String getHeroBadgeText() { return heroBadgeText; }
    public void setHeroBadgeText(String heroBadgeText) { this.heroBadgeText = heroBadgeText; }

    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }

    public String getHeroSubtitle() { return heroSubtitle; }
    public void setHeroSubtitle(String heroSubtitle) { this.heroSubtitle = heroSubtitle; }

    public String getHeroPrimaryBtnText() { return heroPrimaryBtnText; }
    public void setHeroPrimaryBtnText(String heroPrimaryBtnText) { this.heroPrimaryBtnText = heroPrimaryBtnText; }

    public String getHeroSecondaryBtnText() { return heroSecondaryBtnText; }
    public void setHeroSecondaryBtnText(String heroSecondaryBtnText) { this.heroSecondaryBtnText = heroSecondaryBtnText; }

    public BigDecimal getExchangeRateUsdKhr() { return exchangeRateUsdKhr; }
    public void setExchangeRateUsdKhr(BigDecimal exchangeRateUsdKhr) { this.exchangeRateUsdKhr = exchangeRateUsdKhr; }

    public BigDecimal getShippingFlatRate() { return shippingFlatRate; }
    public void setShippingFlatRate(BigDecimal shippingFlatRate) { this.shippingFlatRate = shippingFlatRate; }

    public BigDecimal getShippingFreeThreshold() { return shippingFreeThreshold; }
    public void setShippingFreeThreshold(BigDecimal shippingFreeThreshold) { this.shippingFreeThreshold = shippingFreeThreshold; }

    public BigDecimal getCommissionPercentage() { return commissionPercentage; }
    public void setCommissionPercentage(BigDecimal commissionPercentage) { this.commissionPercentage = commissionPercentage; }

    public BigDecimal getWithdrawalFee() { return withdrawalFee; }
    public void setWithdrawalFee(BigDecimal withdrawalFee) { this.withdrawalFee = withdrawalFee; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getSupportPhone() { return supportPhone; }
    public void setSupportPhone(String supportPhone) { this.supportPhone = supportPhone; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public String getOfficeAddress() { return officeAddress; }
    public void setOfficeAddress(String officeAddress) { this.officeAddress = officeAddress; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }

    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }

    public String getBakongMerchantId() { return bakongMerchantId; }
    public void setBakongMerchantId(String bakongMerchantId) { this.bakongMerchantId = bakongMerchantId; }

    public String getBankApiKey() { return bankApiKey; }
    public void setBankApiKey(String bankApiKey) { this.bankApiKey = bankApiKey; }

    public String getBankApiSecret() { return bankApiSecret; }
    public void setBankApiSecret(String bankApiSecret) { this.bankApiSecret = bankApiSecret; }

    public String getBankWebhookSecret() { return bankWebhookSecret; }
    public void setBankWebhookSecret(String bankWebhookSecret) { this.bankWebhookSecret = bankWebhookSecret; }

    public String getBankEnvironment() { return bankEnvironment; }
    public void setBankEnvironment(String bankEnvironment) { this.bankEnvironment = bankEnvironment; }

    public int getEscrowHoldDays() { return escrowHoldDays; }
    public void setEscrowHoldDays(int escrowHoldDays) { this.escrowHoldDays = escrowHoldDays; }
}
