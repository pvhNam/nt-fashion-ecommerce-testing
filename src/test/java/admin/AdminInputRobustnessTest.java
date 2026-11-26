package admin;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("admin")
@Tag("robustness")
@DisplayName("Admin Robustness — tài nguyên thiếu và ID không hợp lệ")
public class AdminInputRobustnessTest extends AdminRobustnessSupport {
    static Stream<Arguments> invalidIds() {
        return Stream.of(Arguments.of("missing", null), Arguments.of("blank", ""),
                Arguments.of("whitespace", " "), Arguments.of("text", "bad-id"),
                Arguments.of("below-minimum", "-1"), Arguments.of("zero", "0"),
                Arguments.of("integer-overflow", "2147483648"),
                Arguments.of("nonexistent", "2147483647"));
    }

    private String suffix(String id) {
        return id == null ? "" : "&id=" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("ADM-ROB-READ-01 [B08] Admin mở được trang bộ sưu tập")
    void collectionsPageIsAvailable() throws Exception {
        AdminClient client = session("admin");
        Response response = client.get("admin-collections");
        assertAll("B08: collections is a working Admin page",
                () -> assertEquals(200, response.status(), "The collections page must exist in the deployed WAR."),
                () -> assertTrue(response.document().text().toLowerCase(java.util.Locale.ROOT)
                        .contains("bộ sưu tập"), "Missing collections page content."),
                () -> assertHealthyAdmin(client));
    }

    @ParameterizedTest(name = "ADM-ROB-ID-01 [B10] news edit: {0}")
    @MethodSource("invalidIds")
    void newsEditRejectsInvalidId(String label, String id) throws Exception {
        assertTrue(rows("SELECT id FROM news WHERE id=?", Integer.MAX_VALUE).isEmpty(),
                "The reserved nonexistent news ID exists in QA data.");
        AdminClient client = session("admin");
        Response response = client.get("admin-news?type=edit" + suffix(id));
        assertAll("B10: " + label,
                () -> assertControlledRejection(response, "admin-news"),
                () -> assertHealthyAdmin(client));
    }

    @ParameterizedTest(name = "ADM-ROB-ID-02 voucher edit: {0}")
    @MethodSource("invalidIds")
    void voucherEditRejectsInvalidId(String label, String id) throws Exception {
        assertTrue(rows("SELECT id FROM vouchers WHERE id=?", Integer.MAX_VALUE).isEmpty(),
                "The reserved nonexistent voucher ID exists in QA data.");
        AdminClient client = session("admin");
        Response response = client.get("admin-vouchers?action=edit" + suffix(id));
        assertAll("Voucher edit: " + label,
                () -> assertControlledRejection(response, "admin-vouchers"),
                () -> assertHealthyAdmin(client));
    }

    @ParameterizedTest(name = "ADM-ROB-ID-03 [B14] order update: {0}")
    @MethodSource("invalidIds")
    void orderUpdateRejectsInvalidId(String label, String id) throws Exception {
        assertTrue(rows("SELECT id FROM orders WHERE id=?", Integer.MAX_VALUE).isEmpty(),
                "The reserved nonexistent order ID exists in QA data.");
        var before = rows("SELECT id,status FROM orders ORDER BY id");
        Map<String, String> fields = new HashMap<>(Map.of("action", "ship"));
        if (id != null) {
            fields.put("id", id);
        }
        AdminClient client = session("admin");
        Response response = client.post("update-order", fields);
        assertAll("B14: " + label,
                () -> assertControlledRejection(response, "admin-orders"),
                () -> assertEquals(before, rows("SELECT id,status FROM orders ORDER BY id"),
                        "Invalid ID must not update any order."),
                () -> assertHealthyAdmin(client));
    }
}
