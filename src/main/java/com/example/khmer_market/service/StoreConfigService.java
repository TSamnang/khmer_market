package com.example.khmer_market.service;

import com.example.khmer_market.domain.model.StoreConfig;
import com.example.khmer_market.web.dto.StoreConfigDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class StoreConfigService {

    private static final Logger log = LoggerFactory.getLogger(StoreConfigService.class);
    private static final String CONFIG_FILENAME = "store-customization.json";

    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final StoreConfig config = new StoreConfig();

    @Value("${market.business.exchange-rate-usd-khr:4100}")
    private BigDecimal defaultExchangeRate;

    @Value("${market.business.shipping-flat-rate:1.50}")
    private BigDecimal defaultShippingFlatRate;

    @Value("${market.business.shipping-free-threshold:25.00}")
    private BigDecimal defaultShippingFreeThreshold;

    @Value("${market.business.mail-from:support@khmermarket.gov.kh}")
    private String defaultMailFrom;

    @Value("${market.storage.upload-dir:uploads}")
    private String uploadDir;

    public StoreConfigService(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Initialize default configuration
        config.setExchangeRateUsdKhr(defaultExchangeRate != null ? defaultExchangeRate : new BigDecimal("4100"));
        config.setShippingFlatRate(defaultShippingFlatRate != null ? defaultShippingFlatRate : new BigDecimal("1.50"));
        config.setShippingFreeThreshold(defaultShippingFreeThreshold != null ? defaultShippingFreeThreshold : new BigDecimal("25.00"));
        if (defaultMailFrom != null && !defaultMailFrom.isBlank()) {
            config.setSupportEmail(defaultMailFrom);
        }

        // Attempt to load persisted customized config from storage
        loadFromDisk();
    }

    public synchronized StoreConfig getConfig() {
        return this.config;
    }

    public synchronized void updateConfig(StoreConfigDto dto, String adminUsername, String clientIp) {
        if (dto.getStoreName() != null && !dto.getStoreName().isBlank()) {
            config.setStoreName(dto.getStoreName().trim());
        }
        if (dto.getTagline() != null) {
            config.setTagline(dto.getTagline().trim());
        }
        if (dto.getBrandColor() != null && !dto.getBrandColor().isBlank()) {
            config.setBrandColor(dto.getBrandColor().trim());
        }

        config.setAnnouncementEnabled(dto.isAnnouncementEnabled());
        if (dto.getAnnouncementText() != null) {
            config.setAnnouncementText(dto.getAnnouncementText().trim());
        }
        if (dto.getAnnouncementBgClass() != null && !dto.getAnnouncementBgClass().isBlank()) {
            config.setAnnouncementBgClass(dto.getAnnouncementBgClass().trim());
        }

        if (dto.getHeroBadgeText() != null) {
            config.setHeroBadgeText(dto.getHeroBadgeText().trim());
        }
        if (dto.getHeroTitle() != null && !dto.getHeroTitle().isBlank()) {
            config.setHeroTitle(dto.getHeroTitle().trim());
        }
        if (dto.getHeroSubtitle() != null) {
            config.setHeroSubtitle(dto.getHeroSubtitle().trim());
        }
        if (dto.getHeroPrimaryBtnText() != null && !dto.getHeroPrimaryBtnText().isBlank()) {
            config.setHeroPrimaryBtnText(dto.getHeroPrimaryBtnText().trim());
        }
        if (dto.getHeroSecondaryBtnText() != null && !dto.getHeroSecondaryBtnText().isBlank()) {
            config.setHeroSecondaryBtnText(dto.getHeroSecondaryBtnText().trim());
        }

        if (dto.getExchangeRateUsdKhr() != null) {
            config.setExchangeRateUsdKhr(dto.getExchangeRateUsdKhr());
        }
        if (dto.getShippingFlatRate() != null) {
            config.setShippingFlatRate(dto.getShippingFlatRate());
        }
        if (dto.getShippingFreeThreshold() != null) {
            config.setShippingFreeThreshold(dto.getShippingFreeThreshold());
        }
        if (dto.getCommissionPercentage() != null) {
            config.setCommissionPercentage(dto.getCommissionPercentage());
        }
        if (dto.getWithdrawalFee() != null) {
            config.setWithdrawalFee(dto.getWithdrawalFee());
        }

        if (dto.getSupportEmail() != null) {
            config.setSupportEmail(dto.getSupportEmail().trim());
        }
        if (dto.getSupportPhone() != null) {
            config.setSupportPhone(dto.getSupportPhone().trim());
        }
        if (dto.getOperatingHours() != null) {
            config.setOperatingHours(dto.getOperatingHours().trim());
        }
        if (dto.getOfficeAddress() != null) {
            config.setOfficeAddress(dto.getOfficeAddress().trim());
        }

        if (dto.getBankName() != null && !dto.getBankName().isBlank()) {
            config.setBankName(dto.getBankName().trim());
        }
        if (dto.getBankAccountNumber() != null && !dto.getBankAccountNumber().isBlank()) {
            config.setBankAccountNumber(dto.getBankAccountNumber().trim());
        }
        if (dto.getBankAccountName() != null && !dto.getBankAccountName().isBlank()) {
            config.setBankAccountName(dto.getBankAccountName().trim());
        }
        if (dto.getBankBranch() != null) {
            config.setBankBranch(dto.getBankBranch().trim());
        }
        if (dto.getBakongMerchantId() != null) {
            config.setBakongMerchantId(dto.getBakongMerchantId().trim());
        }
        if (dto.getBankApiKey() != null) {
            config.setBankApiKey(dto.getBankApiKey().trim());
        }
        if (dto.getBankApiSecret() != null) {
            config.setBankApiSecret(dto.getBankApiSecret().trim());
        }
        if (dto.getBankWebhookSecret() != null) {
            config.setBankWebhookSecret(dto.getBankWebhookSecret().trim());
        }
        if (dto.getBankEnvironment() != null && !dto.getBankEnvironment().isBlank()) {
            config.setBankEnvironment(dto.getBankEnvironment().trim().toUpperCase());
        }
        if (dto.getEscrowHoldDays() > 0) {
            config.setEscrowHoldDays(dto.getEscrowHoldDays());
        }

        saveToDisk();

        auditService.logAction(
                adminUsername,
                "UPDATE_STORE_CUSTOMIZATION",
                "STORE_CONFIG",
                "GLOBAL",
                clientIp,
                Map.of(
                        "storeName", config.getStoreName(),
                        "bankName", config.getBankName(),
                        "bankAccountNumber", config.getBankAccountNumber(),
                        "escrowHoldDays", config.getEscrowHoldDays()
                )
        );

        log.info("Admin '{}' successfully updated store customization & bank settings.", adminUsername);
    }

    public synchronized void updateBankConfig(StoreConfigDto dto, String adminUsername, String clientIp) {
        if (dto.getBankName() != null && !dto.getBankName().isBlank()) {
            config.setBankName(dto.getBankName().trim());
        }
        if (dto.getBankAccountNumber() != null && !dto.getBankAccountNumber().isBlank()) {
            config.setBankAccountNumber(dto.getBankAccountNumber().trim());
        }
        if (dto.getBankAccountName() != null && !dto.getBankAccountName().isBlank()) {
            config.setBankAccountName(dto.getBankAccountName().trim());
        }
        if (dto.getBankBranch() != null) {
            config.setBankBranch(dto.getBankBranch().trim());
        }
        if (dto.getBakongMerchantId() != null) {
            config.setBakongMerchantId(dto.getBakongMerchantId().trim());
        }
        if (dto.getBankApiKey() != null) {
            config.setBankApiKey(dto.getBankApiKey().trim());
        }
        if (dto.getBankApiSecret() != null) {
            config.setBankApiSecret(dto.getBankApiSecret().trim());
        }
        if (dto.getBankWebhookSecret() != null) {
            config.setBankWebhookSecret(dto.getBankWebhookSecret().trim());
        }
        if (dto.getBankEnvironment() != null && !dto.getBankEnvironment().isBlank()) {
            config.setBankEnvironment(dto.getBankEnvironment().trim().toUpperCase());
        }
        if (dto.getEscrowHoldDays() > 0) {
            config.setEscrowHoldDays(dto.getEscrowHoldDays());
        }
        if (dto.getCommissionPercentage() != null) {
            config.setCommissionPercentage(dto.getCommissionPercentage());
        }

        saveToDisk();

        auditService.logAction(
                adminUsername,
                "UPDATE_PLATFORM_BANK_GATEWAY",
                "PLATFORM_BANK",
                "GLOBAL",
                clientIp,
                Map.of(
                        "bankName", config.getBankName(),
                        "bankAccountNumber", config.getBankAccountNumber(),
                        "bankAccountName", config.getBankAccountName(),
                        "bankEnvironment", config.getBankEnvironment(),
                        "escrowHoldDays", config.getEscrowHoldDays()
                )
        );

        log.info("Admin '{}' successfully updated Platform Master Bank & Gateway credentials.", adminUsername);
    }

    public synchronized void resetToDefaults(String adminUsername, String clientIp) {
        config.setStoreName("Khmer Marketplace");
        config.setTagline("Direct Agricultural Marketplace from Cambodian Farmers");
        config.setBrandColor("#047857");
        config.setAnnouncementEnabled(true);
        config.setAnnouncementText("🎉 Free direct delivery from Cambodian farms across the kingdom for orders over $25! Zero transaction fees with Bakong KHQR.");
        config.setAnnouncementBgClass("bg-success");
        config.setHeroBadgeText("Direct from Cambodian Farmers to Your Table");
        config.setHeroTitle("Fresh Agricultural Harvest Direct to Your Door");
        config.setHeroSubtitle("Support local smallholders from Battambang, Kampot, Takeo, and Mondulkiri with direct farm-to-table delivery.");
        config.setHeroPrimaryBtnText("Browse All Products");
        config.setHeroSecondaryBtnText("Sell as a Farmer");
        config.setExchangeRateUsdKhr(defaultExchangeRate != null ? defaultExchangeRate : new BigDecimal("4100"));
        config.setShippingFlatRate(defaultShippingFlatRate != null ? defaultShippingFlatRate : new BigDecimal("1.50"));
        config.setShippingFreeThreshold(defaultShippingFreeThreshold != null ? defaultShippingFreeThreshold : new BigDecimal("25.00"));
        config.setCommissionPercentage(new BigDecimal("5.00"));
        config.setWithdrawalFee(new BigDecimal("0.25"));
        config.setSupportEmail(defaultMailFrom != null && !defaultMailFrom.isBlank() ? defaultMailFrom : "support@khmermarket.gov.kh");
        config.setSupportPhone("+855 23 888 999");
        config.setOperatingHours("Mon - Sat: 7:00 AM - 6:00 PM");
        config.setOfficeAddress("Sangkat Boeng Keng Kang 1, Khan Boeng Keng Kang, Phnom Penh, Kingdom of Cambodia");

        config.setBankName("ABA Bank");
        config.setBankAccountNumber("001 888 999");
        config.setBankAccountName("KHMER MARKET CO., LTD (ESCROW MASTER)");
        config.setBankBranch("Central Head Office, Phnom Penh");
        config.setBakongMerchantId("khmer_market_master@aba");
        config.setBankApiKey("pk_live_aba_99381748921");
        config.setBankApiSecret("sk_live_aba_sec_884920183749");
        config.setBankWebhookSecret("whsec_aba_928371928374");
        config.setBankEnvironment("LIVE");
        config.setEscrowHoldDays(7);

        saveToDisk();

        auditService.logAction(
                adminUsername,
                "RESET_STORE_CUSTOMIZATION",
                "STORE_CONFIG",
                "GLOBAL",
                clientIp,
                Map.of("action", "Reset all store customization settings to system defaults")
        );

        log.info("Admin '{}' reset store customization to system defaults.", adminUsername);
    }

    public BigDecimal getExchangeRateUsdKhr() {
        return config.getExchangeRateUsdKhr();
    }

    public BigDecimal getShippingFlatRate() {
        return config.getShippingFlatRate();
    }

    public BigDecimal getShippingFreeThreshold() {
        return config.getShippingFreeThreshold();
    }

    public BigDecimal getCommissionPercentage() {
        return config.getCommissionPercentage() != null ? config.getCommissionPercentage() : new BigDecimal("5.00");
    }

    public BigDecimal getWithdrawalFee() {
        return config.getWithdrawalFee() != null ? config.getWithdrawalFee() : new BigDecimal("0.25");
    }

    public String getBankName() {
        return config.getBankName() != null ? config.getBankName() : "ABA Bank";
    }

    public String getBankAccountNumber() {
        return config.getBankAccountNumber() != null ? config.getBankAccountNumber() : "001 888 999";
    }

    public String getBankAccountName() {
        return config.getBankAccountName() != null ? config.getBankAccountName() : "KHMER MARKET CO., LTD (ESCROW MASTER)";
    }

    public String getBakongMerchantId() {
        return config.getBakongMerchantId();
    }

    public String getBankApiKey() {
        return config.getBankApiKey();
    }

    public String getBankApiSecret() {
        return config.getBankApiSecret();
    }

    public String getBankWebhookSecret() {
        return config.getBankWebhookSecret();
    }

    public String getBankEnvironment() {
        return config.getBankEnvironment() != null ? config.getBankEnvironment() : "LIVE";
    }

    public int getEscrowHoldDays() {
        return config.getEscrowHoldDays() > 0 ? config.getEscrowHoldDays() : 7;
    }

    private void saveToDisk() {
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(CONFIG_FILENAME);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), this.config);
            log.debug("Persisted store configuration to: {}", filePath);
        } catch (Exception e) {
            log.warn("Could not persist store customization to disk: {}", e.getMessage());
        }
    }

    private void loadFromDisk() {
        try {
            Path filePath = Paths.get(uploadDir, CONFIG_FILENAME);
            if (Files.exists(filePath)) {
                StoreConfig loaded = objectMapper.readValue(filePath.toFile(), StoreConfig.class);
                if (loaded != null) {
                    if (loaded.getStoreName() != null) config.setStoreName(loaded.getStoreName());
                    if (loaded.getTagline() != null) config.setTagline(loaded.getTagline());
                    if (loaded.getBrandColor() != null) config.setBrandColor(loaded.getBrandColor());
                    config.setAnnouncementEnabled(loaded.isAnnouncementEnabled());
                    if (loaded.getAnnouncementText() != null) config.setAnnouncementText(loaded.getAnnouncementText());
                    if (loaded.getAnnouncementBgClass() != null) config.setAnnouncementBgClass(loaded.getAnnouncementBgClass());
                    if (loaded.getHeroBadgeText() != null) config.setHeroBadgeText(loaded.getHeroBadgeText());
                    if (loaded.getHeroTitle() != null) config.setHeroTitle(loaded.getHeroTitle());
                    if (loaded.getHeroSubtitle() != null) config.setHeroSubtitle(loaded.getHeroSubtitle());
                    if (loaded.getHeroPrimaryBtnText() != null) config.setHeroPrimaryBtnText(loaded.getHeroPrimaryBtnText());
                    if (loaded.getHeroSecondaryBtnText() != null) config.setHeroSecondaryBtnText(loaded.getHeroSecondaryBtnText());
                    if (loaded.getExchangeRateUsdKhr() != null) config.setExchangeRateUsdKhr(loaded.getExchangeRateUsdKhr());
                    if (loaded.getShippingFlatRate() != null) config.setShippingFlatRate(loaded.getShippingFlatRate());
                    if (loaded.getShippingFreeThreshold() != null) config.setShippingFreeThreshold(loaded.getShippingFreeThreshold());
                    if (loaded.getCommissionPercentage() != null) config.setCommissionPercentage(loaded.getCommissionPercentage());
                    if (loaded.getWithdrawalFee() != null) config.setWithdrawalFee(loaded.getWithdrawalFee());
                    if (loaded.getSupportEmail() != null) config.setSupportEmail(loaded.getSupportEmail());
                    if (loaded.getSupportPhone() != null) config.setSupportPhone(loaded.getSupportPhone());
                    if (loaded.getOperatingHours() != null) config.setOperatingHours(loaded.getOperatingHours());
                    if (loaded.getOfficeAddress() != null) config.setOfficeAddress(loaded.getOfficeAddress());

                    if (loaded.getBankName() != null) config.setBankName(loaded.getBankName());
                    if (loaded.getBankAccountNumber() != null) config.setBankAccountNumber(loaded.getBankAccountNumber());
                    if (loaded.getBankAccountName() != null) config.setBankAccountName(loaded.getBankAccountName());
                    if (loaded.getBankBranch() != null) config.setBankBranch(loaded.getBankBranch());
                    if (loaded.getBakongMerchantId() != null) config.setBakongMerchantId(loaded.getBakongMerchantId());
                    if (loaded.getBankApiKey() != null) config.setBankApiKey(loaded.getBankApiKey());
                    if (loaded.getBankApiSecret() != null) config.setBankApiSecret(loaded.getBankApiSecret());
                    if (loaded.getBankWebhookSecret() != null) config.setBankWebhookSecret(loaded.getBankWebhookSecret());
                    if (loaded.getBankEnvironment() != null) config.setBankEnvironment(loaded.getBankEnvironment());
                    if (loaded.getEscrowHoldDays() > 0) config.setEscrowHoldDays(loaded.getEscrowHoldDays());

                    log.info("Loaded custom store configuration from disk: {}", config.getStoreName());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load stored customization from disk, using defaults: {}", e.getMessage());
        }
    }
}
