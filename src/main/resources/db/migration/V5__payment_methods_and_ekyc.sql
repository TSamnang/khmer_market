-- Flyway Migration V5: User Saved Payment Methods and Seller eKYC Verification

-- 1. Add G2G-style eKYC fields to farmer_profiles
ALTER TABLE `farmer_profiles`
  ADD COLUMN `dob` date DEFAULT NULL AFTER `farmer_name`,
  ADD COLUMN `id_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `dob`,
  ADD COLUMN `id_number` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `id_type`,
  ADD COLUMN `id_front_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `farm_image`,
  ADD COLUMN `id_back_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `id_front_image`,
  ADD COLUMN `verification_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' AFTER `id_back_image`,
  ADD COLUMN `rejection_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `verification_status`,
  ADD COLUMN `verified_at` datetime DEFAULT NULL AFTER `rejection_reason`;

-- For existing active farmers, set verification_status to APPROVED
UPDATE `farmer_profiles` fp
  JOIN `users` u ON fp.user_id = u.id
  SET fp.verification_status = 'APPROVED', fp.verified_at = fp.created_at
  WHERE u.status = 'active';

-- 2. Create user_payment_methods table for saved user bank accounts and cards
CREATE TABLE IF NOT EXISTS `user_payment_methods` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `method_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL, -- 'BANK_ACCOUNT', 'CARD', 'BAKONG'
  `provider` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,    -- 'ABA Bank', 'ACLEDA Bank', 'Wing Bank', 'Canadia Bank', 'Bakong', 'Visa', 'Mastercard'
  `account_number` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_holder_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `card_expiry` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_methods_user` (`user_id`),
  CONSTRAINT `fk_payment_methods_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
