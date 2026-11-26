package admin;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("admin")
@Tag("robustness")
@DisplayName("Admin voucher — dữ liệu không hợp lệ và mã trùng")
public class AdminVoucherRobustnessTest extends AdminRobustnessSupport {
    private static final String ROUTE = "admin-vouchers";

    private record Fixture(String code, String description) {}

    private final List<Fixture> fixtures = new ArrayList<>();

    private Fixture newFixture() throws Exception {
        Fixture fixture = new Fixture(marker("QV").toUpperCase(Locale.ROOT), marker("QV_DESC"));
        assertTrue(rows("SELECT id FROM vouchers WHERE code = ?", fixture.code()).isEmpty(),
                "The generated voucher code already exists.");
        fixtures.add(fixture);
        return fixture;
    }

    private Map<String, String> fields(Fixture fixture) {
        Map<String, String> fields = new HashMap<>();
        fields.put("action", "insert");
        fields.put("code", fixture.code());
        fields.put("description", fixture.description());
        fields.put("discountAmount", "1000");
        fields.put("discountType", "FIXED");
        fields.put("minOrder", "20000");
        fields.put("expiryDate", LocalDate.now().plusDays(60).toString());
        return fields;
    }

    private int insertOwnedVoucher(Fixture fixture) throws Exception {
        return insert("INSERT INTO vouchers "
                        + "(code, description, discount_amount, discount_type, min_order, expiry_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                fixture.code(), fixture.description(), 1000.0, "FIXED", 20000.0,
                Date.valueOf(LocalDate.now().plusDays(60)));
    }

    private List<Map<String, Object>> ownedRows(Fixture fixture) throws Exception {
        return rows("SELECT * FROM vouchers WHERE description = ? ORDER BY id",
                fixture.description());
    }

    private void registerSubmittedCode(Fixture fixture, String submittedCode) {
        // A missing/empty code still belongs to this UUID description if a write occurs.
        fixtures.add(new Fixture(submittedCode, fixture.description()));
    }

    private void assertRejectedWithoutChanges(AdminClient admin, Fixture fixture,
            Map<String, String> submitted, List<Map<String, Object>> before) throws Exception {
        registerSubmittedCode(fixture, submitted.get("code"));
        Response response = admin.post(ROUTE, submitted);
        assertAll("Invalid voucher input must be rejected without persisting a change",
                () -> assertControlledRejection(response, ROUTE),
                () -> assertEquals(before, ownedRows(fixture),
                        "The rejected request inserted or changed an owned voucher."),
                () -> assertHealthyAdmin(admin));
    }

    @AfterEach
    void cleanupOwnedVouchers() {
        assertAll("Remove voucher fixtures by exact submitted code and UUID description",
                fixtures.stream().map(fixture -> (Executable) () -> {
                    update("DELETE FROM vouchers WHERE code <=> ? AND description = ?",
                            fixture.code(), fixture.description());
                    assertTrue(rows("SELECT id FROM vouchers WHERE code <=> ? AND description = ?",
                            fixture.code(), fixture.description()).isEmpty(),
                            "An owned voucher remains after cleanup.");
                }));
    }

    @Test
    @DisplayName("VCH-RB-01 B11 mã trùng khi thêm phải báo lỗi và giữ voucher gốc")
    void duplicateInsertPreservesOriginalAndReportsRejection() throws Exception {
        AdminClient admin = session("admin");
        Fixture original = newFixture();
        insertOwnedVoucher(original);
        List<Map<String, Object>> before = ownedRows(original);

        Fixture attempted = new Fixture(original.code(), marker("QV_DUP_DESC"));
        fixtures.add(attempted); // Register the attempted insert before the request.
        Response response = admin.post(ROUTE, fields(attempted));

        assertAll("Duplicate insert must not report success",
                () -> assertControlledRejection(response, ROUTE),
                () -> assertEquals(before, ownedRows(original), "The original voucher changed."),
                () -> assertTrue(ownedRows(attempted).isEmpty(), "The duplicate insert persisted."),
                () -> assertEquals(1,
                        rows("SELECT id FROM vouchers WHERE code = ?", original.code()).size()),
                () -> assertHealthyAdmin(admin));
    }

    @Test
    @DisplayName("VCH-RB-02 B11 đổi mã thành mã đã dùng phải giữ nguyên cả hai voucher")
    void duplicateUpdatePreservesBothOwnedVouchers() throws Exception {
        AdminClient admin = session("admin");
        Fixture original = newFixture();
        Fixture target = newFixture();
        insertOwnedVoucher(original);
        int targetId = insertOwnedVoucher(target);
        List<Map<String, Object>> originalBefore = ownedRows(original);
        List<Map<String, Object>> targetBefore = ownedRows(target);

        Map<String, String> submitted = fields(target);
        submitted.put("action", "update");
        submitted.put("id", String.valueOf(targetId));
        submitted.put("code", original.code());
        registerSubmittedCode(target, original.code());
        Response response = admin.post(ROUTE, submitted);

        assertAll("Duplicate update must be rejected atomically",
                () -> assertControlledRejection(response, ROUTE),
                () -> assertEquals(originalBefore, ownedRows(original),
                        "The voucher owning the duplicate code changed."),
                () -> assertEquals(targetBefore, ownedRows(target),
                        "The attempted update changed its voucher."),
                () -> assertHealthyAdmin(admin));
    }

    static List<Arguments> invalidBusinessValues() {
        return List.of(
                Arguments.of("insert", "FIXED", "discountAmount", "-1"),
                Arguments.of("update", "FIXED", "discountAmount", "-1"),
                Arguments.of("insert", "PERCENT", "discountAmount", "101"),
                Arguments.of("update", "PERCENT", "discountAmount", "101"),
                Arguments.of("insert", "FIXED", "minOrder", "-1"),
                Arguments.of("update", "FIXED", "minOrder", "-1"));
    }

    @ParameterizedTest(name = "VCH-RB-03 {0}, {1}, {2}={3}")
    @MethodSource("invalidBusinessValues")
    @DisplayName("B12 giá trị giảm hoặc đơn tối thiểu ngoài miền phải bị từ chối")
    void invalidBusinessValuesPreserveOwnedState(String action, String type,
            String field, String value) throws Exception {
        AdminClient admin = session("admin");
        Fixture fixture = newFixture();
        Map<String, String> submitted = fields(fixture);
        if ("update".equals(action)) {
            submitted.put("id", String.valueOf(insertOwnedVoucher(fixture)));
        }
        List<Map<String, Object>> before = ownedRows(fixture);
        submitted.put("action", action);
        submitted.put("discountType", type);
        submitted.put(field, value);
        assertRejectedWithoutChanges(admin, fixture, submitted, before);
    }

    static List<Arguments> validBoundaryValues() {
        return List.of(
                Arguments.of("PERCENT", "0"),
                Arguments.of("PERCENT", "1"),
                Arguments.of("PERCENT", "99"),
                Arguments.of("PERCENT", "100"),
                Arguments.of("FIXED", "0"));
    }

    @ParameterizedTest(name = "VCH-RB-04 {0}={1}, minOrder=0")
    @MethodSource("validBoundaryValues")
    @DisplayName("Giá trị biên hợp lệ phải được lưu chính xác")
    void validBoundariesPersist(String type, String amount) throws Exception {
        AdminClient admin = session("admin");
        Fixture fixture = newFixture();
        Map<String, String> submitted = fields(fixture);
        submitted.put("discountType", type);
        submitted.put("discountAmount", amount);
        submitted.put("minOrder", "0");
        Response response = admin.post(ROUTE, submitted);
        assertRedirect(response, ROUTE);

        List<Map<String, Object>> saved = ownedRows(fixture);
        assertEquals(1, saved.size(), "The valid boundary request must persist exactly one row.");
        Map<String, Object> voucher = saved.getFirst();
        assertAll("Stored boundary values",
                () -> assertEquals(fixture.code(), voucher.get("code")),
                () -> assertEquals(type, voucher.get("discount_type")),
                () -> assertEquals(Double.parseDouble(amount),
                        ((Number) voucher.get("discount_amount")).doubleValue()),
                () -> assertEquals(0.0, ((Number) voucher.get("min_order")).doubleValue()),
                () -> assertHealthyAdmin(admin));
    }

    static List<Arguments> invalidNumberValues() {
        List<Arguments> cases = new ArrayList<>();
        for (String field : List.of("discountAmount", "minOrder")) {
            for (String value : List.of("", " ", "abc", "NaN", "Infinity", "-Infinity", "1e309")) {
                cases.add(Arguments.of(field, value));
            }
        }
        return cases;
    }

    @ParameterizedTest(name = "VCH-RB-05 {0}=''{1}''")
    @MethodSource("invalidNumberValues")
    @DisplayName("B13 số rỗng, sai định dạng, không hữu hạn hoặc tràn phải được xử lý")
    void invalidNumbersAreControlled(String field, String value) throws Exception {
        AdminClient admin = session("admin");
        Fixture fixture = newFixture();
        Map<String, String> submitted = fields(fixture);
        submitted.put(field, value);
        assertRejectedWithoutChanges(admin, fixture, submitted, List.of());
    }

    @ParameterizedTest(name = "VCH-RB-06 expiryDate=''{0}''")
    @ValueSource(strings = {"", "not-a-date", "2030-02-30"})
    @DisplayName("B13 ngày rỗng, sai định dạng hoặc không tồn tại phải bị từ chối")
    void invalidExpiryDatesAreControlled(String expiry) throws Exception {
        AdminClient admin = session("admin");
        Fixture fixture = newFixture();
        Map<String, String> submitted = fields(fixture);
        submitted.put("expiryDate", expiry);
        assertRejectedWithoutChanges(admin, fixture, submitted, List.of());
    }

    @ParameterizedTest(name = "VCH-RB-07 thiếu trường {0}")
    @ValueSource(strings = {"action", "code", "discountAmount", "discountType", "minOrder", "expiryDate"})
    @DisplayName("Thiếu trường bắt buộc phải trả lỗi có kiểm soát")
    void missingRequiredFieldsAreControlled(String field) throws Exception {
        AdminClient admin = session("admin");
        Fixture fixture = newFixture();
        Map<String, String> submitted = fields(fixture);
        submitted.remove(field);
        assertRejectedWithoutChanges(admin, fixture, submitted, List.of());
    }

    @Test
    @DisplayName("VCH-RB-08 mã code rỗng phải bị từ chối và không tạo voucher")
    void blankCodeIsRejected() throws Exception {
        AdminClient admin = session("admin");
        Fixture fixture = newFixture();
        Map<String, String> submitted = fields(fixture);
        submitted.put("code", "");
        assertRejectedWithoutChanges(admin, fixture, submitted, List.of());
    }

    static List<Arguments> invalidUpdateValues() {
        return List.of(
                Arguments.of("discountAmount", "NaN"),
                Arguments.of("minOrder", "1e309"),
                Arguments.of("expiryDate", "not-a-date"));
    }

    @ParameterizedTest(name = "VCH-RB-09 update {0}=''{1}''")
    @MethodSource("invalidUpdateValues")
    @DisplayName("B13 đầu vào cập nhật sai phải giữ nguyên voucher hiện có")
    void malformedUpdatesPreserveExistingVoucher(String field, String value) throws Exception {
        AdminClient admin = session("admin");
        Fixture fixture = newFixture();
        int id = insertOwnedVoucher(fixture);
        List<Map<String, Object>> before = ownedRows(fixture);
        Map<String, String> submitted = fields(fixture);
        submitted.put("action", "update");
        submitted.put("id", String.valueOf(id));
        submitted.put(field, value);
        assertRejectedWithoutChanges(admin, fixture, submitted, before);
    }
}
