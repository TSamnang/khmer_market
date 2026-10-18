-- Flyway Migration V4: Platform Master Bank & 7-Day Escrow

ALTER TABLE `orders`
  ADD COLUMN `escrow_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_PAYMENT',
  ADD COLUMN `escrow_release_at` datetime DEFAULT NULL,
  ADD COLUMN `escrow_released_at` datetime DEFAULT NULL;

-- Initialize existing order rows
UPDATE `orders` SET `escrow_status` = 'RELEASED', `escrow_released_at` = `created_at` WHERE `status` = 'delivered';
UPDATE `orders` SET `escrow_status` = 'CANCELLED' WHERE `status` = 'cancelled';
UPDATE `orders` SET `escrow_release_at` = DATE_ADD(`created_at`, INTERVAL 7 DAY) WHERE `escrow_release_at` IS NULL;

CREATE INDEX `idx_orders_escrow_status` ON `orders` (`escrow_status`);
CREATE INDEX `idx_orders_escrow_release_at` ON `orders` (`escrow_release_at`);
