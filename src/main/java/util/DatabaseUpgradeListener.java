package util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import dao.DBConnect;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Tự bổ sung cột/chỉ mục còn thiếu khi web khởi động. Chỉ thêm khi chưa có.
 */
@WebListener
public class DatabaseUpgradeListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try (Connection connection = DBConnect.getConnection()) {
            if (connection == null) {
                sce.getServletContext().log("Không kết nối được DB, bỏ qua bước tự cập nhật cấu trúc bảng.");
                return;
            }
            for (String change : upgrade(connection)) {
                sce.getServletContext().log("Đã tự cập nhật DB: thêm " + change);
            }
        } catch (Exception e) {
            sce.getServletContext().log(
                    "Không tự cập nhật được DB. Hãy chạy các migration còn thiếu theo README.", e);
        }
    }

    /** Thêm những gì còn thiếu; trả về danh sách đã thêm (rỗng nếu DB đã đủ). */
    public static List<String> upgrade(Connection connection) throws SQLException {
        List<String> changes = new ArrayList<>();
        if (!tableExists(connection, "custom_collections") || !tableExists(connection, "custom_collection_products")) {
            runPackagedMigration(connection, "010_custom_collections.sql");
            changes.add("các bảng bộ sưu tập tự tạo");
        }
        if (!tableExists(connection, "hidden_collections")) {
            runPackagedMigration(connection, "012_collection_visibility.sql");
            changes.add("bảng hidden_collections");
        }
        if (!tableExists(connection, "collection_descriptions")) {
            runPackagedMigration(connection, "013_collection_descriptions.sql");
            changes.add("bảng collection_descriptions");
        }
        // ST-16, IS-05: đơn nhớ voucher đã giữ để trả về ví khi thanh toán thất bại/hết hạn
        if (!columnExists(connection, "orders", "voucher_id")) {
            execute(connection, "ALTER TABLE orders ADD COLUMN voucher_id int DEFAULT NULL");
            changes.add("cột orders.voucher_id");
        }
        // Tìm nhanh đơn chờ thanh toán quá hạn
        if (!indexExists(connection, "orders", "idx_orders_status_created")) {
            execute(connection, "ALTER TABLE orders ADD INDEX idx_orders_status_created (status, created_at)");
            changes.add("chỉ mục orders.idx_orders_status_created");
        }
        if (!columnExists(connection, "vouchers", "start_date")) {
            execute(connection, "ALTER TABLE vouchers ADD COLUMN start_date date DEFAULT NULL AFTER min_order");
            changes.add("cột vouchers.start_date");
        }
        if (!columnExists(connection, "orders", "delivered_at")) {
            execute(connection, "ALTER TABLE orders ADD COLUMN delivered_at datetime DEFAULT NULL");
            changes.add("cột orders.delivered_at");
        }
        if (!tableExists(connection, "contact_threads") || !tableExists(connection, "contact_messages")
                || !tableExists(connection, "promotion_campaigns")) {
            runPackagedMigration(connection, "008_contact_and_campaigns.sql");
            changes.add("các bảng Liên hệ và Chiến dịch còn thiếu");
        }
        if (!tableExists(connection, "order_invoices")) {
            runPackagedMigration(connection, "009_customer_invoices.sql");
            changes.add("bảng order_invoices");
        }
        return changes;
    }

    private static void runPackagedMigration(Connection connection, String migration) throws SQLException {
        try (InputStream script = DatabaseUpgradeListener.class.getResourceAsStream(
                "/db-upgrades/" + migration)) {
            if (script == null) throw new SQLException("Không tìm thấy migration " + migration + " trong bản build.");
            String sql = new String(script.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                if (!statement.isBlank()) execute(connection, statement.strip());
            }
        } catch (IOException error) {
            throw new SQLException("Không đọc được migration " + migration, error);
        }
    }

    private static boolean tableExists(Connection connection, String name) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        return exists(connection, "SELECT 1 FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?", table, column);
    }

    private static boolean indexExists(Connection connection, String table, String index) throws SQLException {
        return exists(connection, "SELECT 1 FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?", table, index);
    }

    private static boolean exists(Connection connection, String sql, String table, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
