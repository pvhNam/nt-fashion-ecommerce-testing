package util;

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
 * Tự bổ sung cột/chỉ mục còn thiếu khi web khởi động, để người pull code về không phải chạy tay
 * migrations/005_order_voucher.sql. Chỉ thêm khi chưa có nên chạy lại nhiều lần không sao.
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
                    "Không tự cập nhật được DB, hãy chạy tay migrations/005_order_voucher.sql", e);
        }
    }

    /** Thêm những gì còn thiếu; trả về danh sách đã thêm (rỗng nếu DB đã đủ). */
    public static List<String> upgrade(Connection connection) throws SQLException {
        List<String> changes = new ArrayList<>();
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
        return changes;
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
