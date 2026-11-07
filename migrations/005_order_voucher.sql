-- ================================================
-- Migration: 005_order_voucher.sql
-- Lưu voucher mà đơn hàng đã dùng, để trả voucher về ví khi đơn thanh toán online
-- thất bại hoặc hết hạn chờ thanh toán (ST-16, IS-05)
-- Chạy nhiều lần không bị lỗi: cột kiểm tra information_schema trước khi thêm
-- Web cũng tự làm đúng việc này khi khởi động (util/DatabaseUpgradeListener.java);
-- file này để ghi lại thay đổi DB và để chạy tay nếu tài khoản DB không có quyền ALTER
-- ================================================

SET NAMES utf8mb4;

-- orders.voucher_id: NULL nếu đơn không dùng voucher
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `orders` ADD COLUMN `voucher_id` int DEFAULT NULL',
    'DO 0') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'voucher_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Tìm nhanh đơn chờ thanh toán quá hạn theo trạng thái và thời gian tạo
SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `orders` ADD INDEX `idx_orders_status_created` (`status`, `created_at`)',
    'DO 0') FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_status_created');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
