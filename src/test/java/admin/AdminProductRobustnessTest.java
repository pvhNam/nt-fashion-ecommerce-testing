package admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Product robustness contracts verified through HTTP and the isolated QA database. */
@Tag("admin")
@Tag("robustness")
@DisplayName("Admin robustness — sản phẩm, biến thể và danh mục")
public class AdminProductRobustnessTest extends AdminRobustnessSupport {
    private final Set<String> ownedTriggerNames = new HashSet<>();
    private final Set<String> ownedProductNames = new HashSet<>();
    private final List<OwnedUser> ownedUsers = new ArrayList<>();
    private final List<OwnedCart> ownedCarts = new ArrayList<>();

    private record OwnedProduct(int id, String name) {}
    private record OwnedUser(int id, String username) {}
    private record OwnedCart(int id, int productId, OwnedUser user) {}

    static Stream<Arguments> invalidProductValues() {
        return Stream.of(
                Arguments.of("price", "-1"),
                Arguments.of("price", "not-a-number"),
                Arguments.of("price", "NaN"),
                Arguments.of("price", "Infinity"),
                Arguments.of("price", "-Infinity"),
                Arguments.of("price", "1e309"),
                Arguments.of("price", ""),
                Arguments.of("stock", "-1"),
                Arguments.of("stock", "not-a-number"),
                Arguments.of("stock", "1.5"),
                Arguments.of("stock", "2147483648"),
                Arguments.of("stock", "-2147483649"),
                Arguments.of("stock", ""));
    }

    private int existingCategoryId() throws Exception {
        List<Map<String, Object>> categories = rows("SELECT id FROM category ORDER BY id LIMIT 1");
        assertFalse(categories.isEmpty(), "The QA database requires at least one category.");
        return ((Number) categories.getFirst().get("id")).intValue();
    }

    private String registerProductName(String prefix) throws Exception {
        String name = marker(prefix);
        assertTrue(rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                "An owned product marker must be new.");
        ownedProductNames.add(name);
        return name;
    }

    private OwnedProduct createProduct() throws Exception {
        String name = registerProductName("product");
        int id = insert("INSERT INTO product (name,price,cateID,color,size,amount,img,cost_price) "
                + "VALUES (?,?,?,?,?,?,?,?)", name, 199000, existingCategoryId(), "", "", 2,
                "img/images.jpg", 0);
        return new OwnedProduct(id, name);
    }

    private Map<String, String> productFields(String name) throws Exception {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "add");
        fields.put("name", name);
        fields.put("price", "199000");
        fields.put("cateId", String.valueOf(existingCategoryId()));
        fields.put("color", "");
        fields.put("size", "");
        fields.put("stock", "2");
        fields.put("image", "img/images.jpg");
        return fields;
    }

    private AdminClient verifiedAdmin() throws Exception {
        AdminClient admin = session("admin");
        assertHealthyAdmin(admin);
        return admin;
    }

    private List<Map<String, Object>> productRows(int id) throws Exception {
        return rows("SELECT * FROM product WHERE pid=?", id);
    }

    private int createVariant(OwnedProduct product) throws Exception {
        return insert("INSERT INTO product_variants (product_id,color,size,stock_quantity) VALUES (?,?,?,?)",
                product.id(), "QA Black", "M", 2);
    }

    private String installOwnedGalleryInsertFailure() throws Exception {
        String triggerName = marker("qa_gallery_fault_");
        String imageUrl = marker("img/qa_fail_") + ".jpg";
        assertTrue(triggerName.matches("qa_gallery_fault_[a-f0-9]{32}") && triggerName.length() < 64,
                "The generated trigger identifier is invalid.");
        assertTrue(imageUrl.matches("img/qa_fail_[a-f0-9]{32}\\.jpg"),
                "The generated failure URL is invalid.");
        assertTrue(rows("SELECT id FROM product_images WHERE image_url=?", imageUrl).isEmpty(),
                "The generated failure URL must be new.");
        ownedTriggerNames.add(triggerName); // Track before DDL so teardown covers partial setup.
        String sql = "CREATE TRIGGER `" + triggerName + "` BEFORE INSERT ON product_images "
                + "FOR EACH ROW BEGIN IF NEW.image_url = '" + imageUrl + "' THEN "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'QA owned gallery insert fault'; "
                + "END IF; END";
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
        return imageUrl;
    }

    @AfterEach
    void cleanupOwnedProductFixtures() {
        List<Executable> cleanup = new ArrayList<>();
        for (String triggerName : ownedTriggerNames) {
            cleanup.add(() -> {
                try (var connection = database(); var statement = connection.createStatement()) {
                    statement.execute("DROP TRIGGER IF EXISTS `" + triggerName + "`");
                }
                assertTrue(rows("SELECT TRIGGER_NAME FROM information_schema.TRIGGERS "
                        + "WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME=?", triggerName).isEmpty(),
                        "The owned SQL-failure trigger was not removed.");
            });
        }
        for (OwnedCart cart : ownedCarts) {
            cleanup.add(() -> {
                update("DELETE c FROM cart c JOIN users u ON u.uid=c.user_id "
                        + "WHERE c.id=? AND c.product_id=? AND u.uid=? AND u.username=?",
                        cart.id(), cart.productId(), cart.user().id(), cart.user().username());
                assertTrue(rows("SELECT id FROM cart WHERE id=? AND user_id=? AND product_id=?",
                        cart.id(), cart.user().id(), cart.productId()).isEmpty(),
                        "The owned cart fixture was not removed.");
            });
        }
        for (OwnedUser user : ownedUsers) {
            cleanup.add(() -> {
                update("DELETE FROM users WHERE uid=? AND username=?", user.id(), user.username());
                assertTrue(rows("SELECT uid FROM users WHERE uid=? AND username=?",
                        user.id(), user.username()).isEmpty(), "The owned user fixture was not removed.");
            });
        }
        for (String name : ownedProductNames) {
            cleanup.add(() -> {
                for (Map<String, Object> row : rows("SELECT pid FROM product WHERE name=?", name)) {
                    int id = ((Number) row.get("pid")).intValue();
                    // Related images/variants cascade only from this exact UUID-owned product.
                    update("DELETE FROM product WHERE pid=? AND name=?", id, name);
                }
                assertTrue(rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                        "The owned product fixture was not removed.");
            });
        }
        assertAll("Remove exact UUID-owned product fixtures", cleanup);
    }

    @ParameterizedTest(name = "ADM-B06-CREATE [{index}] {0}={1}")
    @MethodSource("invalidProductValues")
    @DisplayName("ADM-B06-CREATE Giá/tồn không hợp lệ bị từ chối trước khi tạo sản phẩm")
    void rejectsInvalidProductCreation(String field, String value) throws Exception {
        AdminClient admin = verifiedAdmin();
        String name = registerProductName("invalid_product");
        Map<String, String> fields = productFields(name);
        fields.put(field, value);

        Response response = admin.post("admin-products", fields);

        assertAll("Invalid product creation must not persist data",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertTrue(rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                        "An invalid product was persisted."),
                () -> assertHealthyAdmin(admin));
    }

    @ParameterizedTest(name = "ADM-B06-UPDATE [{index}] {0}={1}")
    @MethodSource("invalidProductValues")
    @DisplayName("ADM-B06-UPDATE Giá/tồn không hợp lệ giữ nguyên sản phẩm và ảnh phụ")
    void rejectsInvalidProductUpdateWithoutPartialWrites(String field, String value) throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        insert("INSERT INTO product_images (product_id,image_url) VALUES (?,?)",
                product.id(), "img/images.jpg");
        List<Map<String, Object>> beforeProduct = productRows(product.id());
        List<Map<String, Object>> beforeImages = rows(
                "SELECT * FROM product_images WHERE product_id=? ORDER BY id", product.id());
        String changedName = product.name() + "_changed";
        ownedProductNames.add(changedName);
        Map<String, String> fields = productFields(changedName);
        fields.put("action", "update");
        fields.put("pid", String.valueOf(product.id()));
        fields.put(field, value);

        Response response = admin.post("admin-products", fields);

        assertAll("Invalid product update must be atomic",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertEquals(beforeProduct, productRows(product.id()),
                        "An invalid update changed the stored product."),
                () -> assertEquals(beforeImages, rows(
                        "SELECT * FROM product_images WHERE product_id=? ORDER BY id", product.id()),
                        "An invalid update changed related images."),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("FIX01-UPDATE Sửa sản phẩm hợp lệ lưu giá trị mới và thay toàn bộ bộ ảnh phụ")
    void validProductUpdateReplacesProductValuesAndGallery() throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        insert("INSERT INTO product_images (product_id,image_url) VALUES (?,?)",
                product.id(), "img/old-gallery-one.jpg");
        insert("INSERT INTO product_images (product_id,image_url) VALUES (?,?)",
                product.id(), "img/old-gallery-two.jpg");
        String changedName = registerProductName("updated_product");
        int categoryId = existingCategoryId();
        Map<String, String> fields = productFields(changedName);
        fields.put("action", "update");
        fields.put("pid", String.valueOf(product.id()));
        fields.put("price", "249000.5");
        fields.put("stock", "4");
        fields.put("color", "QA Navy");
        fields.put("size", "L");
        fields.put("image", "img/updated-main.jpg");
        fields.put("extraImages", "img/replacement-gallery.jpg");

        Response response = admin.post("admin-products", fields);

        assertRedirect(response, "admin-products");
        waitUntil(() -> rows("SELECT pid FROM product WHERE pid=? AND name=?",
                product.id(), changedName).size() == 1, "The valid product update was not persisted.");
        List<Map<String, Object>> saved = productRows(product.id());
        assertEquals(1, saved.size(), "A valid update must preserve the existing product ID.");
        Map<String, Object> row = saved.getFirst();
        assertAll("The valid update must persist all values and replace the gallery",
                () -> assertEquals(changedName, row.get("name")),
                () -> assertEquals(249000.5, ((Number) row.get("price")).doubleValue()),
                () -> assertEquals(categoryId, ((Number) row.get("cateid")).intValue()),
                () -> assertEquals(4, ((Number) row.get("amount")).intValue()),
                () -> assertEquals("QA Navy", row.get("color")),
                () -> assertEquals("L", row.get("size")),
                () -> assertEquals("img/updated-main.jpg", row.get("img")),
                () -> assertTrue(rows("SELECT pid FROM product WHERE name=?", product.name()).isEmpty(),
                        "The old product name still exists after updating the owned product."),
                () -> assertEquals(List.of(Map.of("image_url", "img/replacement-gallery.jpg")),
                        rows("SELECT image_url FROM product_images WHERE product_id=? ORDER BY id", product.id()),
                        "The old gallery must be replaced by the submitted image."),
                () -> assertHealthyAdmin(admin));
    }

    @ParameterizedTest(name = "FIX01-MISSING [{index}] action={0}, missing={1}")
    @CsvSource({"add,price", "add,stock", "update,price", "update,stock"})
    @DisplayName("FIX01-MISSING Thiếu giá hoặc tồn phải bị từ chối và giữ nguyên dữ liệu")
    void missingProductNumbersDoNotDefaultToZero(String action, String missingField) throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = null;
        List<Map<String, Object>> beforeProduct = List.of();
        List<Map<String, Object>> beforeImages = List.of();
        String name = registerProductName("missing_product_number");
        Map<String, String> fields = productFields(name);
        fields.put("action", action);
        fields.put("extraImages", "img/missing-number-gallery.jpg");
        if ("update".equals(action)) {
            product = createProduct();
            insert("INSERT INTO product_images (product_id,image_url) VALUES (?,?)",
                    product.id(), "img/original-number-gallery.jpg");
            fields.put("pid", String.valueOf(product.id()));
            beforeProduct = productRows(product.id());
            beforeImages = rows("SELECT * FROM product_images WHERE product_id=? ORDER BY id", product.id());
        }
        fields.remove(missingField);

        Response response = admin.post("admin-products", fields);

        OwnedProduct existing = product;
        List<Map<String, Object>> expectedProduct = beforeProduct;
        List<Map<String, Object>> expectedImages = beforeImages;
        assertAll("A missing numeric field must not become a persisted zero",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertTrue(rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                        "The missing-field request persisted the submitted product name."),
                () -> {
                    if (existing != null) {
                        assertEquals(expectedProduct, productRows(existing.id()),
                                "The missing-field update changed its product.");
                        assertEquals(expectedImages, rows(
                                "SELECT * FROM product_images WHERE product_id=? ORDER BY id", existing.id()),
                                "The missing-field update changed its gallery.");
                    }
                },
                () -> assertHealthyAdmin(admin));
    }

    @ParameterizedTest(name = "FIX01-LEGACY-INVALID [{index}] {0}={1}")
    @MethodSource("invalidProductValues")
    @DisplayName("FIX01-LEGACY-INVALID Endpoint thêm cũ phải từ chối cùng miền giá và tồn không hợp lệ")
    void legacyProductCreationRejectsInvalidNumbers(String field, String value) throws Exception {
        AdminClient admin = verifiedAdmin();
        String name = registerProductName("legacy_invalid_product");
        Map<String, String> fields = productFields(name);
        fields.put(field, value);

        Response response = admin.post("admin-add-product", fields);

        assertAll("Legacy product creation must enforce the same numeric validation",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertTrue(rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                        "The legacy endpoint persisted an invalid product."),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("FIX01-LEGACY-BOUNDARY Endpoint thêm cũ lưu đúng giá 0 và tồn 0")
    void legacyProductCreationAcceptsZeroPriceAndStock() throws Exception {
        AdminClient admin = verifiedAdmin();
        String name = registerProductName("legacy_boundary_product");
        Map<String, String> fields = productFields(name);
        fields.put("price", "0");
        fields.put("stock", "0");

        Response response = admin.post("admin-add-product", fields);

        assertRedirect(response, "admin-products");
        waitUntil(() -> !rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                "The valid legacy boundary product was not persisted.");
        List<Map<String, Object>> saved = rows("SELECT price,amount FROM product WHERE name=?", name);
        assertEquals(1, saved.size(), "The legacy endpoint must create exactly one boundary product.");
        assertAll("Legacy zero values must round-trip exactly",
                () -> assertEquals(0.0, ((Number) saved.getFirst().get("price")).doubleValue()),
                () -> assertEquals(0, ((Number) saved.getFirst().get("amount")).intValue()),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("FIX01-NOT-FOUND Cập nhật ID không tồn tại phải báo lỗi và không đổi bộ ảnh")
    void nonexistentProductUpdateDoesNotReportSuccessOrChangeGalleries() throws Exception {
        AdminClient admin = verifiedAdmin();
        int missingId = Integer.MAX_VALUE;
        assertTrue(productRows(missingId).isEmpty(), "The missing-ID fixture precondition was not met.");
        assertTrue(rows("SELECT id FROM product_images WHERE product_id=?", missingId).isEmpty(),
                "The missing product ID already has gallery rows.");
        OwnedProduct witness = createProduct();
        insert("INSERT INTO product_images (product_id,image_url) VALUES (?,?)",
                witness.id(), "img/unrelated-owned-gallery.jpg");
        List<Map<String, Object>> beforeProduct = productRows(witness.id());
        List<Map<String, Object>> beforeImages = rows(
                "SELECT * FROM product_images WHERE product_id=? ORDER BY id", witness.id());
        String name = registerProductName("nonexistent_product_update");
        Map<String, String> fields = productFields(name);
        fields.put("action", "update");
        fields.put("pid", String.valueOf(missingId));
        fields.put("extraImages", "img/missing-product-gallery.jpg");

        Response response = admin.post("admin-products", fields);

        assertAll("Updating a nonexistent product must not report success or change galleries",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertTrue(productRows(missingId).isEmpty(), "The missing-ID update created a product."),
                () -> assertTrue(rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                        "The missing-ID update persisted the submitted product name."),
                () -> assertTrue(rows("SELECT id FROM product_images WHERE product_id=?", missingId).isEmpty(),
                        "The missing-ID update inserted gallery rows."),
                () -> assertEquals(beforeProduct, productRows(witness.id()),
                        "The missing-ID update changed another owned product."),
                () -> assertEquals(beforeImages, rows(
                        "SELECT * FROM product_images WHERE product_id=? ORDER BY id", witness.id()),
                        "The missing-ID update changed another owned gallery."),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("FIX01-CREATE-ROLLBACK Lỗi SQL khi ghi ảnh phải rollback cả sản phẩm mới")
    void gallerySqlFailureRollsBackProductCreation() throws Exception {
        AdminClient admin = verifiedAdmin();
        String failureImage = installOwnedGalleryInsertFailure();
        String name = registerProductName("gallery_create_rollback");
        Map<String, String> fields = productFields(name);
        fields.put("extraImages", failureImage);

        Response response = admin.post("admin-products", fields);

        assertAll("A gallery SQL failure must roll back the earlier product insert",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertTrue(rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                        "The product insert remained after the gallery SQL failure."),
                () -> assertTrue(rows("SELECT id FROM product_images WHERE image_url=?", failureImage).isEmpty(),
                        "A gallery row remained after the failed transaction."),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("FIX01-UPDATE-ROLLBACK Lỗi SQL khi thay bộ ảnh phải khôi phục sản phẩm và ảnh cũ")
    void gallerySqlFailureRollsBackProductAndGalleryUpdate() throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        insert("INSERT INTO product_images (product_id,image_url) VALUES (?,?)",
                product.id(), "img/gallery-before-rollback.jpg");
        List<Map<String, Object>> beforeProduct = productRows(product.id());
        List<Map<String, Object>> beforeImages = rows(
                "SELECT * FROM product_images WHERE product_id=? ORDER BY id", product.id());
        String failureImage = installOwnedGalleryInsertFailure();
        String changedName = registerProductName("gallery_update_rollback");
        Map<String, String> fields = productFields(changedName);
        fields.put("action", "update");
        fields.put("pid", String.valueOf(product.id()));
        fields.put("price", "299000");
        fields.put("stock", "7");
        fields.put("extraImages", failureImage);

        Response response = admin.post("admin-products", fields);

        assertAll("A gallery SQL failure must roll back product changes and gallery deletion",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertEquals(beforeProduct, productRows(product.id()),
                        "The product changes were committed before the gallery SQL failure."),
                () -> assertEquals(beforeImages, rows(
                        "SELECT * FROM product_images WHERE product_id=? ORDER BY id", product.id()),
                        "The previous gallery was deleted despite the SQL failure."),
                () -> assertTrue(rows("SELECT pid FROM product WHERE name=?", changedName).isEmpty(),
                        "The changed name remained after rollback."),
                () -> assertTrue(rows("SELECT id FROM product_images WHERE image_url=?", failureImage).isEmpty(),
                        "The failed image was persisted."),
                () -> assertHealthyAdmin(admin));
    }

    @ParameterizedTest(name = "ADM-B06-BOUNDARY [{index}] price={0}, stock={1}")
    @CsvSource({"0,0", "0,1", "1,0", "1,1", "1,2147483647"})
    @DisplayName("ADM-B06-BOUNDARY Giá/tồn bằng 0, 1 và tồn tối đa int được lưu đúng")
    void acceptsNonNegativeProductBoundaries(String price, String stock) throws Exception {
        AdminClient admin = verifiedAdmin();
        String name = registerProductName("boundary_product");
        Map<String, String> fields = productFields(name);
        fields.put("price", price);
        fields.put("stock", stock);

        Response response = admin.post("admin-products", fields);

        assertRedirect(response, "admin-products");
        waitUntil(() -> !rows("SELECT pid FROM product WHERE name=?", name).isEmpty(),
                "The valid boundary product was not persisted.");
        List<Map<String, Object>> saved = rows("SELECT price,amount FROM product WHERE name=?", name);
        assertEquals(1, saved.size(), "Expected exactly one boundary fixture.");
        assertAll("Boundary values must round-trip exactly",
                () -> assertEquals(Double.parseDouble(price), ((Number) saved.getFirst().get("price")).doubleValue()),
                () -> assertEquals(Integer.parseInt(stock), ((Number) saved.getFirst().get("amount")).intValue()));
    }

    @ParameterizedTest(name = "ADM-B06-VARIANT [{index}] newQty={0}")
    @ValueSource(strings = {"-1", "not-a-number", "1.5", "2147483648", "-2147483649", "NaN", ""})
    @DisplayName("ADM-B06-VARIANT Số lượng biến thể không hợp lệ giữ nguyên cả tồn biến thể và tổng tồn")
    void rejectsInvalidVariantQuantityWithoutChangingStock(String quantity) throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        int variantId = createVariant(product);
        List<Map<String, Object>> beforeProduct = productRows(product.id());
        List<Map<String, Object>> beforeVariant = rows("SELECT * FROM product_variants WHERE id=?", variantId);

        Response response = admin.post("admin-variants", Map.of(
                "action", "update-qty", "pid", String.valueOf(product.id()),
                "vid", String.valueOf(variantId), "newQty", quantity));

        assertAll("Invalid variant quantity must not affect persisted stock",
                () -> assertControlledRejection(response, "admin-variants"),
                () -> assertEquals(beforeVariant, rows("SELECT * FROM product_variants WHERE id=?", variantId),
                        "An invalid quantity changed the variant."),
                () -> assertEquals(beforeProduct, productRows(product.id()),
                        "An invalid quantity changed aggregate product stock."),
                () -> assertHealthyAdmin(admin));
    }

    @ParameterizedTest(name = "ADM-B06-VARIANT-BOUNDARY [{index}] newQty={0}")
    @ValueSource(ints = {0, 1, 2, Integer.MAX_VALUE})
    @DisplayName("ADM-B06-VARIANT-BOUNDARY Biên hợp lệ và cập nhật không đổi đồng bộ đúng tổng tồn")
    void acceptsVariantQuantityBoundariesAndSynchronizesStock(int quantity) throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        int variantId = createVariant(product);

        Response response = admin.post("admin-variants", Map.of(
                "action", "update-qty", "pid", String.valueOf(product.id()),
                "vid", String.valueOf(variantId), "newQty", String.valueOf(quantity)));

        assertRedirect(response, "admin-variants");
        waitUntil(() -> ((Number) rows("SELECT stock_quantity FROM product_variants WHERE id=?", variantId)
                .getFirst().get("stock_quantity")).intValue() == quantity,
                "The valid variant quantity was not persisted.");
        assertEquals(quantity, ((Number) productRows(product.id()).getFirst().get("amount")).intValue(),
                "The product's total stock must equal its sole variant.");
    }

    @Test
    @DisplayName("FIX02-MISSING Thiếu số lượng phải từ chối và giữ nguyên tồn kho")
    void missingVariantQuantityPreservesStock() throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        int variantId = createVariant(product);
        var beforeProduct = productRows(product.id());
        var beforeVariant = rows("SELECT * FROM product_variants WHERE id=?", variantId);
        Response response = admin.post("admin-variants", Map.of("action", "update-qty",
                "pid", String.valueOf(product.id()), "vid", String.valueOf(variantId)));
        assertAll("Missing quantity is not zero",
                () -> assertControlledRejection(response, "admin-variants"),
                () -> assertEquals(beforeProduct, productRows(product.id())),
                () -> assertEquals(beforeVariant, rows("SELECT * FROM product_variants WHERE id=?", variantId)),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("FIX02-NOT-FOUND Biến thể không tồn tại không được báo thành công")
    void nonexistentVariantDoesNotReportSuccessfulStockUpdate() throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        int variantId = createVariant(product);
        assertTrue(rows("SELECT id FROM product_variants WHERE id=?", Integer.MAX_VALUE).isEmpty());
        var beforeProduct = productRows(product.id());
        var beforeVariant = rows("SELECT * FROM product_variants WHERE id=?", variantId);
        Response response = admin.post("admin-variants", Map.of("action", "update-qty",
                "pid", String.valueOf(product.id()), "vid", String.valueOf(Integer.MAX_VALUE), "newQty", "1"));
        assertAll("Missing variant must not claim a successful update",
                () -> assertControlledRejection(response, "admin-variants"),
                () -> assertEquals(beforeProduct, productRows(product.id())),
                () -> assertEquals(beforeVariant, rows("SELECT * FROM product_variants WHERE id=?", variantId)),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("FIX02-ROLLBACK Lỗi đồng bộ tổng tồn phải rollback số lượng biến thể")
    void aggregateStockSqlFailureRollsBackVariantUpdate() throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        int variantId = createVariant(product);
        var beforeProduct = productRows(product.id());
        var beforeVariant = rows("SELECT * FROM product_variants WHERE id=?", variantId);
        String triggerName = marker("qa_variant_fault_");
        assertTrue(triggerName.matches("qa_variant_fault_[a-f0-9]{32}"));
        ownedTriggerNames.add(triggerName);
        try (var connection = database(); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER `" + triggerName + "` BEFORE UPDATE ON product "
                    + "FOR EACH ROW BEGIN IF OLD.pid=" + product.id() + " AND NEW.amount<>OLD.amount THEN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA owned stock sync fault'; END IF; END");
        }
        Response response = admin.post("admin-variants", Map.of("action", "update-qty",
                "pid", String.valueOf(product.id()), "vid", String.valueOf(variantId), "newQty", "1"));
        assertAll("Stock synchronization failure must roll back the earlier variant write",
                () -> assertControlledRejection(response, "admin-variants"),
                () -> assertEquals(beforeProduct, productRows(product.id())),
                () -> assertEquals(beforeVariant, rows("SELECT * FROM product_variants WHERE id=?", variantId)),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("ADM-B07 Xóa sản phẩm bị khóa ngoại chặn phải báo lỗi và giữ nguyên dữ liệu")
    void referencedProductDeletionDoesNotReportFalseSuccess() throws Exception {
        AdminClient admin = verifiedAdmin();
        OwnedProduct product = createProduct();
        String username = marker("fk_user");
        int userId = insert("INSERT INTO users (username,password,email,fullname,phonenumber,is_admin) "
                + "VALUES (?,?,NULL,?,NULL,0)", username, "unusable-qa-fixture", username);
        OwnedUser user = new OwnedUser(userId, username);
        ownedUsers.add(user);
        int cartId = insert("INSERT INTO cart (user_id,product_id,quantity,size,color) VALUES (?,?,1,'','')",
                user.id(), product.id());
        ownedCarts.add(new OwnedCart(cartId, product.id(), user));
        List<Map<String, Object>> beforeProduct = productRows(product.id());

        Response response = admin.post("admin-products", Map.of(
                "action", "delete", "pid", String.valueOf(product.id())));

        assertAll("A referenced product must not be reported deleted",
                () -> assertControlledRejection(response, "admin-products"),
                () -> assertEquals(beforeProduct, productRows(product.id()),
                        "The FK-protected product must remain unchanged."),
                () -> assertEquals(1, rows("SELECT id FROM cart WHERE id=? AND user_id=? AND product_id=?",
                        cartId, user.id(), product.id()).size(), "The owned reference must remain intact."),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("ADM-B09 Danh mục trong form sản phẩm phải khớp các ID thật trong database")
    void productFormCategoriesMatchDatabase() throws Exception {
        AdminClient admin = verifiedAdmin();
        Set<Integer> databaseIds = new HashSet<>();
        for (Map<String, Object> row : rows("SELECT id FROM category")) {
            databaseIds.add(((Number) row.get("id")).intValue());
        }
        assertFalse(databaseIds.isEmpty(), "The QA database requires category fixtures.");

        Response response = admin.get("admin-products?type=add");

        assertEquals(200, response.status(), "The product form did not load.");
        Element select = response.document().selectFirst("select[name=cateId]");
        assertNotNull(select, "The form is missing its category selector.");
        Set<Integer> optionIds = new HashSet<>();
        for (Element option : select.select("option[value]")) {
            if (!option.val().isBlank()) {
                assertTrue(option.val().matches("[0-9]+"), "A category option has a nonnumeric ID.");
                optionIds.add(Integer.parseInt(option.val()));
            }
        }
        assertEquals(databaseIds, optionIds, "The product form must offer the current database category IDs.");
    }
}
