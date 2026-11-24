-- ================================================
-- Migration: 006_voucher_start_date.sql
-- Ngày bắt đầu của voucher: NULL là dùng được ngay; trước ngày này khách lưu được vào ví nhưng chưa áp dụng được
-- Chạy nhiều lần không bị lỗi. Web cũng tự thêm cột này khi khởi động (util/DatabaseUpgradeListener.java)
-- ================================================

SET NAMES utf8mb4;

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `vouchers` ADD COLUMN `start_date` date DEFAULT NULL AFTER `min_order`',
    'DO 0') FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vouchers' AND COLUMN_NAME = 'start_date');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
