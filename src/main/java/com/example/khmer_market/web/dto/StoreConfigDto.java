package com.example.khmer_market.web.dto;

import com.example.khmer_market.domain.model.StoreConfig;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class StoreConfigDto {

    @NotBlank(message = "Store name is required")
    @Size(max = 100, message = "Store name cannot exceed 100 characters")
    private String storeName;

    @Size(max = 200, message = "Tagline cannot exceed 200 characters")
    private String tagline;

    private String brandColor = "#047857";

    private boolean announcementEnabled;

    @Size(max = 300, message = "Announcement text cannot exceed 300 characters")
    private String announcementText;

    private String announcementBgClass = "bg-success";

    @Size(max = 150, message = "Hero badge cannot exceed 150 characters")
    private String heroBadgeText;

    @NotBlank(message = "Hero title is required")
    @Size(max = 150, message = "Hero title cannot exceed 150 characters")
    private String heroTitle;

    @Size(max = 300, message = "Hero subtitle cannot exceed 300 characters")
    private String heroSubtitle;

    private String heroPrimaryBtnText = "Browse All Products";
    private String heroSecondaryBtnText = "Sell as a Farmer";

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "100.00", message = "Exchange rate must be at least 100 KHR per USD")
    private BigDecimal exchangeRateUsdKhr;

    @NotNull(message = "Shipping flat rate is required")
    @DecimalMin(value = "0.00", message = "Shipping flat rate cannot be negative")
    private BigDecimal shippingFlatRate;

    @NotNull(message = "Free shipping threshold is required")
    @DecimalMin(value = "0.00", message = "Free shipping threshold cannot be negative")
    private BigDecimal shippingFreeThreshold;

    @NotNull(message = "Platform commission percentage is required")
    @DecimalMin(value = "0.00", message = "Commission percentage cannot be negative")
    private BigDecimal commissionPercentage = new BigDecimal("5.00");

    @NotNull(message = "Withdrawal processing fee is required")
    @DecimalMin(value = "0.00", message = "Withdrawal fee cannot be negative")
    private BigDecimal withdrawalFee = new BigDecimal("0.25");

    @Size(max = 120, message = "Email cannot exceed 120 characters")
    private String supportEmail;

    @Size(max = 40, message = "Phone cannot exceed 40 characters")
    private String supportPhone;

    @Size(max = 150, message = "Operating hours cannot exceed 150 characters")
    private String operatingHours;

    @Size(max = 250, message = "Office address cannot exceed 250 characters")
    private String officeAddress;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name cannot exceed 100 characters")
    private String bankName = "ABA Bank";

    @NotBlank(message = "Bank account number is required")
    @Size(max = 50, message = "Account number cannot exceed 50 characters")
    private String bankAccountNumber = "001 888 999";

    @NotBlank(message = "Bank account name is required")
    @Size(max = 120, message = "Account name cannot exceed 120 characters")
    private String bankAccountName = "KHMER MARKET CO., LTD (ESCROW MASTER)";

    @Size(max = 150, message = "Branch name cannot exceed 150 characters")
    private String bankBranch = "Central Head Office, Phnom Penh";

    @Size(max = 100, message = "Bakong Merchant ID cannot exceed 100 characters")
    private String bakongMerchantId = "khmer_market_master@aba";

    @Size(max = 150, message = "API key cannot exceed 150 characters")
    private String bankApiKey = "pk_live_aba_99381748921";

    @Size(max = 150, message = "API secret cannot exceed 150 characters")
    private String bankApiSecret = "sk_live_aba_sec_884920183749";

    @Size(max = 150, message = "Webhook secret cannot exceed 150 characters")
    private String bankWebhookSecret = "whsec_aba_928371928374";

    private String bankEnvironment = "LIVE";

    private int escrowHoldDays = 7;

    public StoreConfigDto() {}

    public static StoreConfigDto fromConfig(StoreConfig config) {
        StoreConfigDto dto = new StoreConfigDto();
        dto.setStoreName(config.getStoreName());
        dto.setTagline(config.getTagline());
        dto.setBrandColor(config.getBrandColor());
        dto.setAnnouncementEnabled(config.isAnnouncementEnabled());
        dto.setAnnouncementText(config.getAnnouncementText());
        dto.setAnnouncementBgClass(config.getAnnouncementBgClass());
        dto.setHeroBadgeText(config.getHeroBadgeText());
        dto.setHeroTitle(config.getHeroTitle());
        dto.setHeroSubtitle(config.getHeroSubtitle());
        dto.setHeroPrimaryBtnText(config.getHeroPrimaryBtnText());
        dto.setHeroSecondaryBtnText(config.getHeroSecondaryBtnText());
        dto.setExchangeRateUsdKhr(config.getExchangeRateUsdKhr());
        dto.setShippingFlatRate(config.getShippingFlatRate());
        dto.setShippingFreeThreshold(config.getShippingFreeThreshold());
        dto.setCommissionPercentage(config.getCommissionPercentage() != null ? config.getCommissionPercentage() : new BigDecimal("5.00"));
        dto.setWithdrawalFee(config.getWithdrawalFee() != null ? config.getWithdrawalFee() : new BigDecimal("0.25"));
        dto.setSupportEmail(config.getSupportEmail());
        dto.setSupportPhone(config.getSupportPhone());
        dto.setOperatingHours(config.getOperatingHours());
        dto.setOfficeAddress(config.getOfficeAddress());

        dto.setBankName(config.getBankName() != null ? config.getBankName() : "ABA Bank");
        dto.setBankAccountNumber(config.getBankAccountNumber() != null ? config.getBankAccountNumber() : "001 888 999");
        dto.setBankAccountName(config.getBankAccountName() != null ? config.getBankAccountName() : "KHMER MARKET CO., LTD (ESCROW MASTER)");
        dto.setBankBranch(config.getBankBranch());
        dto.setBakongMerchantId(config.getBakongMerchantId());
        dto.setBankApiKey(config.getBankApiKey());
        dto.setBankApiSecret(config.getBankApiSecret());
        dto.setBankWebhookSecret(config.getBankWebhookSecret());
        dto.setBankEnvironment(config.getBankEnvironment() != null ? config.getBankEnvironment() : "LIVE");
        dto.setEscrowHoldDays(config.getEscrowHoldDays() > 0 ? config.getEscrowHoldDays() : 7);
        return dto;
    }

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
