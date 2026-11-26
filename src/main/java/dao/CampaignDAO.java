package dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CampaignDAO {
    public record Campaign(int id, String title, String description, LocalDate start, LocalDate end,
                           Integer voucherId, String voucherCode, boolean published) {
        public String status(LocalDate today) {
            if (!published) return "Tạm dừng";
            if (start.isAfter(today)) return "Sắp diễn ra";
            if (end.isBefore(today)) return "Đã kết thúc";
            return "Đang diễn ra";
        }
    }

    public List<Campaign> list(boolean publicOnly) throws SQLException {
        String sql = "SELECT p.*, v.code FROM promotion_campaigns p LEFT JOIN vouchers v ON v.id = p.voucher_id "
                + (publicOnly ? "AND v.is_public = 1 AND (v.start_date IS NULL OR v.start_date <= CURRENT_DATE) "
                        + "AND (v.expiry_date IS NULL OR v.expiry_date >= CURRENT_DATE) "
                        + "WHERE p.published = 1 AND CURRENT_DATE BETWEEN p.starts_on AND p.ends_on " : "")
                + "ORDER BY p.starts_on DESC, p.id DESC";
        try (Connection connection = connect(); PreparedStatement query = connection.prepareStatement(sql);
             ResultSet rows = query.executeQuery()) {
            List<Campaign> result = new ArrayList<>();
            while (rows.next()) result.add(new Campaign(rows.getInt("id"), rows.getString("title"),
                    rows.getString("description"), rows.getObject("starts_on", LocalDate.class),
                    rows.getObject("ends_on", LocalDate.class), rows.getObject("voucher_id", Integer.class),
                    rows.getString("code"), rows.getBoolean("published")));
            return result;
        }
    }

    public boolean save(Campaign campaign) throws SQLException {
        String sql = campaign.id() == 0
                ? "INSERT INTO promotion_campaigns (title, description, starts_on, ends_on, voucher_id, published) VALUES (?, ?, ?, ?, ?, ?)"
                : "UPDATE promotion_campaigns SET title=?, description=?, starts_on=?, ends_on=?, voucher_id=?, published=? WHERE id=?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, campaign.title());
            statement.setString(2, campaign.description());
            statement.setObject(3, campaign.start());
            statement.setObject(4, campaign.end());
            statement.setObject(5, campaign.voucherId(), Types.INTEGER);
            statement.setBoolean(6, campaign.published());
            if (campaign.id() > 0) statement.setInt(7, campaign.id());
            return statement.executeUpdate() == 1;
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DBConnect.getConnection();
        if (connection == null) throw new SQLException("Cannot connect to database");
        return connection;
    }
}
