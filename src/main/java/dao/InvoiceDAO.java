package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InvoiceDAO {
    public record Buyer(String kind, String name, String address, String taxCode) {
        public boolean isCompany() { return "company".equals(kind); }
    }

    public Buyer find(int orderId) throws SQLException {
        String sql = "SELECT kind, buyer_name, buyer_address, tax_code FROM order_invoices WHERE order_id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new Buyer(result.getString("kind"), result.getString("buyer_name"),
                        result.getString("buyer_address"), result.getString("tax_code"));
            }
        }
    }

    public void save(int orderId, Buyer buyer) throws SQLException {
        String sql = "INSERT INTO order_invoices (order_id, kind, buyer_name, buyer_address, tax_code) "
                + "VALUES (?, ?, ?, ?, ?) AS updated ON DUPLICATE KEY UPDATE kind = updated.kind, "
                + "buyer_name = updated.buyer_name, buyer_address = updated.buyer_address, tax_code = updated.tax_code";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setString(2, buyer.kind());
            statement.setString(3, buyer.name());
            statement.setString(4, buyer.address());
            statement.setString(5, buyer.taxCode());
            statement.executeUpdate();
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DBConnect.getConnection();
        if (connection == null) throw new SQLException("Cannot connect to database");
        return connection;
    }
}
