-- ================================================
-- Migration: 004_collections_loyalty.sql
-- Bảng/cột cho bộ sưu tập, hạng thành viên và thông tin liên hệ đơn hàng
-- (CollectionDAO, LoyaltyDAO, VoucherDAO, OrderDAO)
-- Chạy nhiều lần không bị lỗi: bảng dùng IF NOT EXISTS, cột kiểm tra information_schema trước khi thêm
-- ================================================

SET NAMES utf8mb4;

-- Sản phẩm thuộc bộ sưu tập nào (theme_slug lấy từ enum CollectionTheme)
CREATE TABLE IF NOT EXISTS `product_collections` (
    `product_id` int NOT NULL,
    `theme_slug` varchar(50) NOT NULL,
    PRIMARY KEY (`product_id`, `theme_slug`),
    KEY `idx_product_collections_theme` (`theme_slug`),
    CONSTRAINT `fk_product_collections_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`pid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Hạng thành viên: mỗi user một dòng
CREATE TABLE IF NOT EXISTS `customer_memberships` (
    `user_id`          int NOT NULL,
    `tier`             varchar(20) NOT NULL DEFAULT 'STANDARD',
    `lifetime_spend`   double NOT NULL DEFAULT '0',
    `discount_percent` double NOT NULL DEFAULT '0',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_customer_memberships_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Voucher tri ân 100k: mỗi đơn hoàn tất chỉ được tặng một lần
CREATE TABLE IF NOT EXISTS `loyalty_order_rewards` (
    `id`         int NOT NULL AUTO_INCREMENT,
    `order_id`   int NOT NULL,
    `user_id`    int NOT NULL,
    `voucher_id` int NOT NULL,
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_loyalty_order_rewards_order` (`order_id`),
    KEY `user_id` (`user_id`),
    KEY `voucher_id` (`voucher_id`),
    CONSTRAINT `fk_loyalty_order_rewards_order`   FOREIGN KEY (`order_id`)   REFERENCES `orders`   (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_loyalty_order_rewards_user`    FOREIGN KEY (`user_id`)    REFERENCES `users`    (`uid`) ON DELETE CASCADE,
    CONSTRAINT `fk_loyalty_order_rewards_voucher` FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- vouchers.is_public: voucher cũ hiện trên trang chủ (mặc định 1), voucher tri ân riêng tư (0)
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `vouchers` ADD COLUMN `is_public` tinyint(1) NOT NULL DEFAULT 1',
    'DO 0') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vouchers' AND COLUMN_NAME = 'is_public');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- orders: email/sđt liên hệ khi đặt hàng và % giảm giá thành viên đã áp dụng
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `orders` ADD COLUMN `contact_email` varchar(100) DEFAULT NULL',
    'DO 0') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'contact_email');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `orders` ADD COLUMN `contact_phone` varchar(20) DEFAULT NULL',
    'DO 0') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'contact_phone');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `orders` ADD COLUMN `member_discount_percent` double NOT NULL DEFAULT 0',
    'DO 0') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'member_discount_percent');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
