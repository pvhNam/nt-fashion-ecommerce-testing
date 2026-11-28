package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.CartItem;
import model.Category;
import model.Product;
import model.ProductVariant;

public class ProductDAO {
    private static final String AVAILABLE_STOCK_SQL = "SELECT CASE "
            + "WHEN EXISTS (SELECT 1 FROM product_variants pv0 WHERE pv0.product_id = p.pid) "
            + "THEN COALESCE((SELECT SUM(pv.stock_quantity) FROM product_variants pv "
            + "WHERE pv.product_id = p.pid AND pv.color = ? AND pv.size = ?), 0) "
            + "ELSE GREATEST(COALESCE(p.amount, 0), 0) END AS available_stock "
            + "FROM product p WHERE p.pid = ?";

    // LẤY DANH SÁCH BIẾN THỂ (VARIANT) THEO SẢN PHẨM
    public List<ProductVariant> getVariantsByProductId(int pid) {
        List<ProductVariant> list = new ArrayList<>();
        String sql = "SELECT * FROM product_variants WHERE product_id = ? ORDER BY id";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new ProductVariant(rs.getInt("id"), rs.getInt("product_id"),
                        rs.getString("color"), rs.getString("size"), rs.getInt("stock_quantity")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // LẤY DANH SÁCH ẢNH PHỤ CỦA SẢN PHẨM
    public List<String> getExtraImages(int pid) {
        List<String> images = new ArrayList<>();
        String sql = "SELECT image_url FROM product_images WHERE product_id = ?";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                images.add(rs.getString("image_url"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return images;
    }

    // Lấy tất cả sản phẩm
    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product ORDER BY pid";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product(rs.getInt("pid"), rs.getString("name"), rs.getDouble("price"), rs.getInt("cateID"),
                        rs.getString("color"), rs.getString("size"), rs.getInt("amount"), rs.getString("img"));
                p.setCostPrice(rs.getDouble("cost_price"));
                attachVariantsAndEffectiveStock(p);
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // TÌM KIẾM SẢN PHẨM THEO TÊN
    public List<Product> searchProduct(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product WHERE name LIKE ? ORDER BY pid";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%"); // Tìm gần đúng
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product(rs.getInt("pid"), rs.getString("name"), rs.getDouble("price"),
                            rs.getInt("cateID"), rs.getString("color"), rs.getString("size"), rs.getInt("amount"),
                            rs.getString("img"));
                    p.setCostPrice(rs.getDouble("cost_price"));
                    attachVariantsAndEffectiveStock(p);
                    list.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Lấy sản phẩm theo ID
    public Product getProductById(int pid) {
        String sql = "SELECT * FROM product WHERE pid = ?";
        try (Connection conn = DBConnect.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = new Product(rs.getInt("pid"), rs.getString("name"), rs.getDouble("price"),
                            rs.getInt("cateID"), rs.getString("color"), rs.getString("size"), rs.getInt("amount"),
                            rs.getString("img"));
                    p.setCostPrice(rs.getDouble("cost_price"));

                    attachVariantsAndEffectiveStock(p);
                    p.setExtraImages(getExtraImages(p.getPid())); // Lấy ảnh phụ

                    return p;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Thêm sản phẩm
    public boolean addProduct(Product p) {
        return addProductReturnId(p) > 0;
    }

    public int addProductReturnId(Product p) {
        return addProductWithImages(p, null);
    }

    /** Creates the product and its gallery as one committed change. */
    public int addProductWithImages(Product product, String[] imageUrls) {
        if (!validProductNumbers(product)) {
            return -1;
        }
        return inTransaction(connection -> {
            int productId = insertProductRow(connection, product);
            replaceExtraImages(connection, productId, imageUrls);
            return productId;
        });
    }

    // Cập nhật sản phẩm
    public boolean updateProduct(Product p) {
        if (!validProductNumbers(p) || p.getPid() <= 0) {
            return false;
        }
        return inTransaction(connection -> updateProductRow(connection, p)) > 0;
    }

    /** Updates the product and replaces its gallery in the same transaction. */
    public boolean updateProductWithImages(Product product, String[] imageUrls) {
        if (!validProductNumbers(product) || product.getPid() <= 0) {
            return false;
        }
        return inTransaction(connection -> {
            int affectedRows = updateProductRow(connection, product);
            if (affectedRows == 0) {
                return 0;
            }
            replaceExtraImages(connection, product.getPid(), imageUrls);
            return affectedRows;
        }) > 0;
    }

    // Cập nhật bộ ảnh phụ (Xóa ảnh cũ, thêm ảnh mới)
    public void updateExtraImages(int pid, String[] imageUrls) {
        if (pid <= 0) {
            return;
        }
        inTransaction(connection -> {
            // Serialize gallery-only calls with product updates, including an empty gallery.
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pid FROM product WHERE pid=? FOR UPDATE")) {
                statement.setInt(1, pid);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return 0;
                    }
                }
            }
            replaceExtraImages(connection, pid, imageUrls);
            return 1;
        });
    }

    private boolean validProductNumbers(Product product) {
        return product != null && Double.isFinite(product.getPrice()) && product.getPrice() >= 0
                && product.getStockquantyti() >= 0;
    }

    private int insertProductRow(Connection connection, Product product) throws SQLException {
        String sql = "INSERT INTO product (name, price, cateID, color, size, amount, img) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProduct(statement, product);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Product insert did not create exactly one row.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next() || keys.getInt(1) <= 0) {
                    throw new SQLException("Product insert did not return a valid generated ID.");
                }
                return keys.getInt(1);
            }
        }
    }

    private int updateProductRow(Connection connection, Product product) throws SQLException {
        String sql = "UPDATE product SET name=?, price=?, cateID=?, color=?, size=?, amount=?, img=? WHERE pid=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProduct(statement, product);
            statement.setInt(8, product.getPid());
            return statement.executeUpdate();
        }
    }

    private void bindProduct(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getPdescription());
        statement.setDouble(2, product.getPrice());
        statement.setInt(3, product.getCid());
        statement.setString(4, product.getColor());
        statement.setString(5, product.getSize());
        statement.setInt(6, product.getStockquantyti());
        statement.setString(7, product.getImage());
    }

    private void replaceExtraImages(Connection connection, int productId, String[] imageUrls)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM product_images WHERE product_id=?")) {
            statement.setInt(1, productId);
            statement.executeUpdate();
        }
        if (imageUrls == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO product_images (product_id,image_url) VALUES (?,?)")) {
            for (String imageUrl : imageUrls) {
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    statement.setInt(1, productId);
                    statement.setString(2, imageUrl.trim());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Gallery insert did not create exactly one row.");
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface TransactionWork {
        int execute(Connection connection) throws SQLException;
    }

    private int inTransaction(TransactionWork work) {
        try (Connection connection = DBConnect.getConnection()) {
            if (connection == null) {
                return -1;
            }
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            boolean transactionFinished = false;
            try {
                int result = work.execute(connection);
                if (result > 0) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                transactionFinished = true;
                return result;
            } catch (SQLException | RuntimeException error) {
                try {
                    connection.rollback();
                    transactionFinished = true;
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                error.printStackTrace();
                return -1;
            } finally {
                // Enabling autocommit after a failed rollback could commit partial writes.
                if (transactionFinished && originalAutoCommit) {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException restoreError) {
                        restoreError.printStackTrace();
                    }
                }
            }
        } catch (SQLException error) {
            error.printStackTrace();
            return -1;
        }
    }

    public enum DeleteResult {
        DELETED, NOT_FOUND, REFERENCED, FAILED
    }

    // MySQL rejects a parent-row delete when a foreign key still references it.
    private static final int MYSQL_ROW_IS_REFERENCED = 1451;

    public DeleteResult deleteProduct(int pid) {
        String sql = "DELETE FROM product WHERE pid=?";
        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) {
                return DeleteResult.FAILED;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pid);
                return ps.executeUpdate() > 0 ? DeleteResult.DELETED : DeleteResult.NOT_FOUND;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_ROW_IS_REFERENCED) {
                return DeleteResult.REFERENCED;
            }
            e.printStackTrace();
            return DeleteResult.FAILED;
        }
    }

    // Cập nhật số lượng của 1 biến thể cụ thể
    public boolean updateVariantQuantity(int variantId, int newQty) {
        if (variantId <= 0 || newQty < 0) {
            return false;
        }
        String sqlUpdateVar = "UPDATE product_variants SET stock_quantity = ? WHERE id = ?";
        String sqlSyncTotal = "UPDATE product p SET amount = (SELECT SUM(stock_quantity) FROM product_variants WHERE product_id = p.pid) " +
                "WHERE pid = (SELECT product_id FROM product_variants WHERE id = ?)";
        try (Connection conn = DBConnect.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try (PreparedStatement lock = conn.prepareStatement(
                    "SELECT v.id FROM product_variants v JOIN product p ON p.pid=v.product_id "
                            + "WHERE v.id=? FOR UPDATE")) {
                lock.setInt(1, variantId);
                try (ResultSet result = lock.executeQuery()) {
                    if (!result.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }
            try (PreparedStatement ps1 = conn.prepareStatement(sqlUpdateVar);
                 PreparedStatement ps2 = conn.prepareStatement(sqlSyncTotal)) {

                ps1.setInt(1, newQty);
                ps1.setInt(2, variantId);
                ps1.executeUpdate();

                ps2.setInt(1, variantId);
                ps2.executeUpdate();

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // Xóa một biến thể (màu/size) khỏi kho
    public boolean deleteVariant(int variantId) {
        String sqlGetPid = "SELECT product_id FROM product_variants WHERE id = ?";
        String sqlDelete = "DELETE FROM product_variants WHERE id = ?";
        String sqlSyncTotal = "UPDATE product SET amount = (SELECT COALESCE(SUM(stock_quantity), 0) FROM product_variants WHERE product_id = ?) WHERE pid = ?";

        try (Connection conn = DBConnect.getConnection()) {
            conn.setAutoCommit(false);
            int pid = -1;
            try (PreparedStatement psPid = conn.prepareStatement(sqlGetPid)) {
                psPid.setInt(1, variantId);
                ResultSet rs = psPid.executeQuery();
                if (rs.next()) pid = rs.getInt("product_id");
            }

            try (PreparedStatement psDel = conn.prepareStatement(sqlDelete);
                 PreparedStatement psSync = conn.prepareStatement(sqlSyncTotal)) {

                psDel.setInt(1, variantId);
                psDel.executeUpdate();

                if (pid != -1) {
                    psSync.setInt(1, pid);
                    psSync.setInt(2, pid);
                    psSync.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public List<Product> getProductsByCategory(String categoryName) {
        List<Product> list = new ArrayList<>();
        boolean useCategory = categoryName != null && !"all".equalsIgnoreCase(categoryName) && !categoryName.trim().isEmpty();
        String lower = categoryName != null ? categoryName.toLowerCase() : null;

        PreparedStatement ps = null;
        try (Connection conn = DBConnect.getConnection()) {
            String sql;
            if (!useCategory || "bộ sưu tập 2026".equalsIgnoreCase(lower) || "bo suu tap 2026".equalsIgnoreCase(lower)) {
                sql = "SELECT * FROM product";
                ps = conn.prepareStatement(sql);
            } else if ("áo nam".equalsIgnoreCase(lower) || "ao nam".equalsIgnoreCase(lower) || "áo".equalsIgnoreCase(lower) || "ao".equalsIgnoreCase(lower)) {
                // as_ci: phân biệt dấu để "áo" không khớp "Thao", "Cao"
                sql = "SELECT * FROM product WHERE (name COLLATE utf8mb4_0900_as_ci LIKE ? ) " +
                      "AND name COLLATE utf8mb4_0900_as_ci NOT LIKE ? AND name COLLATE utf8mb4_0900_as_ci NOT LIKE ? " +
                      "AND name COLLATE utf8mb4_0900_as_ci NOT LIKE ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, "%áo%");
                ps.setString(2, "%nữ%");
                ps.setString(3, "%ghim%");
                ps.setString(4, "%vớ%");
            } else if ("quần nam".equalsIgnoreCase(lower) || "quan nam".equalsIgnoreCase(lower)) {
                sql = "SELECT * FROM product WHERE (LOWER(name) LIKE ? OR LOWER(name) LIKE ?) AND LOWER(name) LIKE ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, "%quần%");
                ps.setString(2, "%quan%");
                ps.setString(3, "%nam%");
            } else if ("phụ kiện".equalsIgnoreCase(lower) || "phu kien".equalsIgnoreCase(lower)) {
                sql = "SELECT * FROM product WHERE NOT ( (LOWER(name) LIKE ? OR LOWER(name) LIKE ?) AND LOWER(name) LIKE ? ) " +
                      "AND NOT ( (LOWER(name) LIKE ? OR LOWER(name) LIKE ?) AND LOWER(name) LIKE ? )";
                ps = conn.prepareStatement(sql);
                ps.setString(1, "%áo%");
                ps.setString(2, "%ao%");
                ps.setString(3, "%nam%");
                ps.setString(4, "%quần%");
                ps.setString(5, "%quan%");
                ps.setString(6, "%nam%");
            } else {
                sql = "SELECT * FROM product WHERE LOWER(name) LIKE ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, "%" + lower + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product(
                            rs.getInt("pid"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            rs.getInt("cateID"),
                            rs.getString("color"),
                            rs.getString("size"),
                            rs.getInt("amount"),
                            rs.getString("img")
                    );
                    p.setCostPrice(rs.getDouble("cost_price"));
                    attachVariantsAndEffectiveStock(p);
                    list.add(p);
                }
            } finally {
                try { if (ps != null) ps.close(); } catch (Exception ex) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Products created before variant inventory was introduced still use product.amount.
     * As soon as a product has variants, their sum is the authoritative stock value.
     */
    private void attachVariantsAndEffectiveStock(Product product) {
        List<ProductVariant> variants = getVariantsByProductId(product.getPid());
        product.setVariants(variants);
        if (!variants.isEmpty()) {
            int total = 0;
            for (ProductVariant variant : variants) {
                total += Math.max(0, variant.getStockQuantity());
            }
            product.setStockquantyti(total);
        }
    }

    /**
     * Returns stock for the exact selected size/color. For legacy products without
     * variants, product.amount remains the stock source. A missing product or an
     * invalid variant selection has zero available stock.
     */
    public int getAvailableStock(int pid, String color, String size) {
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(AVAILABLE_STOCK_SQL)) {
            return readAvailableStock(ps, pid, color, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Map<CartItem, Integer> getGuestCartStock(List<CartItem> cart) {
        Map<CartItem, Integer> stock = new HashMap<>();
        if (cart.isEmpty()) return stock;
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(AVAILABLE_STOCK_SQL)) {
            for (CartItem item : cart) {
                int available = readAvailableStock(ps, item.getProduct().getPid(), item.getColor(), item.getSize());
                stock.put(item, available);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stock;
    }

    private int readAvailableStock(PreparedStatement ps, int pid, String color, String size) throws SQLException {
        ps.setString(1, normalizeVariantValue(color));
        ps.setString(2, normalizeVariantValue(size));
        ps.setInt(3, pid);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Math.max(0, rs.getInt("available_stock")) : 0;
        }
    }

    private String normalizeVariantValue(String value) {
        return value == null ? "" : value.trim();
    }
}
