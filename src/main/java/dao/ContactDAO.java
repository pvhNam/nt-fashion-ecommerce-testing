package dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {
    public record Thread(int id, String subject, String customer, LocalDateTime updatedAt, boolean answered) {}
    public record Message(boolean fromAdmin, String body, LocalDateTime createdAt) {}

    public List<Thread> list(int ownerId, boolean admin) throws SQLException {
        String sql = "SELECT t.id, t.subject, t.updated_at, u.fullname, "
                + "(SELECT m.from_admin FROM contact_messages m WHERE m.thread_id=t.id ORDER BY m.id DESC LIMIT 1) AS answered "
                + "FROM contact_threads t "
                + "JOIN users u ON u.uid = t.user_id " + (admin ? "" : "WHERE t.user_id = ? ")
                + "ORDER BY t.updated_at DESC, t.id DESC";
        try (Connection connection = connect(); PreparedStatement query = connection.prepareStatement(sql)) {
            if (!admin) query.setInt(1, ownerId);
            try (ResultSet result = query.executeQuery()) {
                List<Thread> threads = new ArrayList<>();
                while (result.next()) threads.add(new Thread(result.getInt("id"), result.getString("subject"),
                        result.getString("fullname"), result.getObject("updated_at", LocalDateTime.class),
                        result.getBoolean("answered")));
                return threads;
            }
        }
    }

    public List<Message> messages(int threadId, int ownerId, boolean admin) throws SQLException {
        String sql = "SELECT m.from_admin, m.body, m.created_at FROM contact_messages m "
                + "JOIN contact_threads t ON t.id = m.thread_id WHERE t.id = ? "
                + (admin ? "" : "AND t.user_id = ? ") + "ORDER BY m.id";
        try (Connection connection = connect(); PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, threadId);
            if (!admin) query.setInt(2, ownerId);
            try (ResultSet result = query.executeQuery()) {
                List<Message> messages = new ArrayList<>();
                while (result.next()) messages.add(new Message(result.getBoolean("from_admin"),
                        result.getString("body"), result.getObject("created_at", LocalDateTime.class)));
                return messages;
            }
        }
    }

    public int send(int threadId, int ownerId, boolean admin, String subject, String body) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                if (threadId == 0 && !admin) {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO contact_threads (user_id, subject) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                        insert.setInt(1, ownerId);
                        insert.setString(2, subject);
                        insert.executeUpdate();
                        try (ResultSet keys = insert.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("Missing contact thread ID");
                            threadId = keys.getInt(1);
                        }
                    }
                } else {
                    String sql = "SELECT id FROM contact_threads WHERE id = ? "
                            + (admin ? "" : "AND user_id = ? ") + "FOR UPDATE";
                    try (PreparedStatement query = connection.prepareStatement(sql)) {
                        query.setInt(1, threadId);
                        if (!admin) query.setInt(2, ownerId);
                        try (ResultSet result = query.executeQuery()) {
                            if (!result.next()) {
                                connection.rollback();
                                return 0;
                            }
                        }
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO contact_messages (thread_id, from_admin, body) VALUES (?, ?, ?)");
                     PreparedStatement touch = connection.prepareStatement(
                             "UPDATE contact_threads SET updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    insert.setInt(1, threadId);
                    insert.setBoolean(2, admin);
                    insert.setString(3, body);
                    insert.executeUpdate();
                    touch.setInt(1, threadId);
                    touch.executeUpdate();
                }
                connection.commit();
                return threadId;
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DBConnect.getConnection();
        if (connection == null) throw new SQLException("Cannot connect to database");
        return connection;
    }
}
