package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import model.CollectionTheme;
import model.Product;

public class CollectionDAO {
    private Connection connection() throws SQLException {
        Connection connection = DBConnect.getConnection();
        if (connection == null) throw new SQLException("Database connection unavailable");
        return connection;
    }

    public List<CollectionTheme> getThemes() throws SQLException {
        List<CollectionTheme> themes = new ArrayList<>(CollectionTheme.builtIn());
        try (Connection connection = connection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT id, title FROM custom_collections ORDER BY id");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    themes.add(CollectionTheme.custom(result.getLong("id"), result.getString("title")));
                }
            }
            Map<String, String> descriptions = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT theme_slug, description FROM collection_descriptions");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    descriptions.put(result.getString("theme_slug"), result.getString("description"));
                }
            }
            themes.replaceAll(theme -> descriptions.containsKey(theme.getSlug())
                    ? theme.withDescription(descriptions.get(theme.getSlug())) : theme);
        }
        return themes;
    }

    public CollectionTheme createTheme(String title, String description) throws SQLException {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                CollectionTheme theme;
                try (PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO custom_collections (title) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, title);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Missing generated collection ID");
                        theme = CollectionTheme.custom(keys.getLong(1), title);
                    }
                }
                if (!description.isEmpty()) saveDescription(connection, theme.getSlug(), description);
                connection.commit();
                return theme;
            } catch (SQLException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }

    public void saveDescription(String slug, String description) throws SQLException {
        try (Connection connection = connection()) {
            saveDescription(connection, slug, description);
        }
    }

    private void saveDescription(Connection connection, String slug, String description) throws SQLException {
        String sql = "INSERT INTO collection_descriptions (theme_slug, description) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE description = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            statement.setString(2, description);
            statement.setString(3, description);
            statement.executeUpdate();
        }
    }

    public Set<String> getHiddenSlugs() throws SQLException {
        Set<String> slugs = new HashSet<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT theme_slug FROM hidden_collections");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) slugs.add(result.getString("theme_slug"));
        }
        return slugs;
    }

    public void setVisible(String slug, boolean visible) throws SQLException {
        String sql = visible
                ? "DELETE FROM hidden_collections WHERE theme_slug = ?"
                : "INSERT INTO hidden_collections (theme_slug) VALUES (?) ON DUPLICATE KEY UPDATE theme_slug = theme_slug";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            statement.executeUpdate();
        }
    }

    public Map<String, Integer> getCounts() throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT pc.theme_slug, COUNT(*) AS total FROM product_collections pc "
                     + "JOIN product p ON p.pid = pc.product_id GROUP BY pc.theme_slug "
                     + "UNION ALL SELECT CONCAT('custom-', collection_id), COUNT(*) "
                     + "FROM custom_collection_products GROUP BY collection_id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) counts.put(result.getString("theme_slug"), result.getInt("total"));
        }
        return counts;
    }

    public List<Product> getProducts(CollectionTheme theme) throws SQLException {
        return getProducts(theme, true);
    }

    public List<Product> getAvailableProducts(CollectionTheme theme) throws SQLException {
        return getProducts(theme, false);
    }

    private List<Product> getProducts(CollectionTheme theme, boolean assigned) throws SQLException {
        Long customId = theme == null ? null : theme.getCustomId();
        String legacyMembership = "EXISTS (SELECT 1 FROM product_collections pc WHERE pc.product_id = p.pid";
        String customMembership = "EXISTS (SELECT 1 FROM custom_collection_products cp WHERE cp.product_id = p.pid";
        String filter;
        if (theme == null) {
            // The storefront's combined view includes products from visible collections only.
            filter = legacyMembership + " AND NOT EXISTS (SELECT 1 FROM hidden_collections h "
                    + "WHERE h.theme_slug = CONVERT(pc.theme_slug USING utf8mb4) COLLATE utf8mb4_0900_ai_ci)) OR "
                    + customMembership + " AND NOT EXISTS (SELECT 1 FROM hidden_collections h "
                    + "WHERE h.theme_slug = CONCAT('custom-', cp.collection_id)))";
        } else {
            filter = (assigned ? "" : "NOT ") + (customId == null
                    ? legacyMembership + " AND pc.theme_slug = ?)"
                    : customMembership + " AND cp.collection_id = ?)");
        }
        String sql = "SELECT p.* FROM product p WHERE " + filter + " ORDER BY p.pid DESC";
        List<Product> products = new ArrayList<>();
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (customId != null) {
                statement.setLong(1, customId);
            } else if (theme != null) {
                statement.setString(1, theme.getSlug());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) products.add(new Product(result.getInt("pid"), result.getString("name"),
                        result.getDouble("price"), result.getInt("cateID"), result.getString("color"),
                        result.getString("size"), result.getInt("amount"), result.getString("img")));
            }
        }
        return products;
    }

    public void addProduct(CollectionTheme theme, int productId) throws SQLException {
        String sql = theme.getCustomId() == null
                ? "INSERT INTO product_collections (product_id, theme_slug) VALUES (?, ?) ON DUPLICATE KEY UPDATE theme_slug = theme_slug"
                : "INSERT INTO custom_collection_products (product_id, collection_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE collection_id = collection_id";
        updateMembership(sql, theme, productId);
    }

    public void removeProduct(CollectionTheme theme, int productId) throws SQLException {
        String sql = theme.getCustomId() == null
                ? "DELETE FROM product_collections WHERE product_id = ? AND theme_slug = ?"
                : "DELETE FROM custom_collection_products WHERE product_id = ? AND collection_id = ?";
        updateMembership(sql, theme, productId);
    }

    private void updateMembership(String sql, CollectionTheme theme, int productId) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, productId);
            if (theme.getCustomId() == null) {
                statement.setString(2, theme.getSlug());
            } else {
                statement.setLong(2, theme.getCustomId());
            }
            statement.executeUpdate();
        }
    }
}
