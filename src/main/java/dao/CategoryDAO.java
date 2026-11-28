package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Category;

public class CategoryDAO {
    public List<Category> findAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM category ORDER BY name, id");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Category category = new Category();
                category.setId(rows.getInt("id"));
                category.setName(rows.getString("name"));
                categories.add(category);
            }
        }
        return categories;
    }

    public boolean exists(int id) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM category WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private Connection connection() throws SQLException {
        Connection connection = DBConnect.getConnection();
        if (connection == null) throw new SQLException("Database connection unavailable");
        return connection;
    }
}