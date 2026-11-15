package admin;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@Tag("admin")
@DisplayName("Admin — tạo, sửa và xóa dữ liệu thử nghiệm")
public class AdminCrudTest extends AdminTestSupport {
    private static final class Fixture {
        final AdminClient client;
        final String route;
        final String marker;
        String id;

        Fixture(AdminClient client, String route, String marker) {
            this.client = client;
            this.route = route;
            this.marker = marker;
        }
    }

    private final List<Fixture> fixtures = new ArrayList<>();

    @AfterEach
    void cleanupFixtures() {
        assertAll("Cleanup owned fixtures",
                fixtures.stream().map(fixture -> (Executable) () -> deleteFixture(fixture)));
    }

    private Document listing(Fixture fixture) throws Exception {
        Response response = fixture.client.get(fixture.route);
        assertEquals(200, response.status(), "Fixture list failed to load.");
        return response.document();
    }

    private List<Element> records(Document page, String route) {
        return "admin-news".equals(route) ? page.select("table.admin-table tbody tr")
                : page.select("div.voucher-card");
    }

    private List<String> ownedIds(Fixture fixture) throws Exception {
        List<String> ids = new ArrayList<>();
        for (Element record : records(listing(fixture), fixture.route)) {
            boolean matches = record.getAllElements().stream()
                    .anyMatch(element -> fixture.marker.equals(element.ownText().trim()));
            if (matches) {
                var inputs = record.select("input[name=id]");
                assertEquals(1, inputs.size(), "The owned fixture ID is missing or ambiguous.");
                String id = inputs.first().val();
                assertTrue(id.matches("[0-9]+"), "Fixture ID must be numeric.");
                ids.add(id);
            }
        }
        return ids;
    }

    private Fixture registerFixture(String route, String marker) throws Exception {
        Fixture fixture = new Fixture(session("admin"), route, marker);
        assertTrue(ownedIds(fixture).isEmpty(), "Fixture marker already exists.");
        fixtures.add(fixture); // Register before the POST so unexpected writes are also cleaned.
        return fixture;
    }

    private void captureCreatedId(Fixture fixture) throws Exception {
        waitUntil(() -> !ownedIds(fixture).isEmpty(), "The newly created fixture was not persisted.");
        List<String> ids = ownedIds(fixture);
        assertEquals(1, ids.size(), "Expected exactly one new fixture.");
        fixture.id = ids.getFirst();
    }

    private boolean containsId(Document page, String id) {
        return page.select("input[name=id]").stream().anyMatch(input -> id.equals(input.val()));
    }

    private void deleteFixture(Fixture fixture) throws Exception {
        if (fixture.id == null) {
            List<String> ids = ownedIds(fixture);
            if (ids.isEmpty()) {
                return;
            }
            assertEquals(1, ids.size(), "Fixture ownership is ambiguous.");
            fixture.id = ids.getFirst();
        }
        if (!containsId(listing(fixture), fixture.id)) {
            assertTrue(ownedIds(fixture).isEmpty(), "Owned fixture remains under another ID.");
            return;
        }
        String actionKey = "admin-news".equals(fixture.route) ? "type" : "action";
        assertRedirect(fixture.client.post(fixture.route,
                Map.of(actionKey, "delete", "id", fixture.id)), fixture.route);
        waitUntil(() -> !containsId(listing(fixture), fixture.id)
                        && ownedIds(fixture).isEmpty(),
                "The owned fixture still exists after deletion.");
    }

    @Test
    @DisplayName("ADM-CRUD-01 Admin tạo, đọc, sửa và xóa tin tức")
    void newsCreateUpdateDelete() throws Exception {
        Fixture fixture = registerFixture("admin-news",
                "QA_ADMIN_NEWS_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        Map<String, String> fields = new HashMap<>(Map.of(
                "title", fixture.marker, "shortDesc", "QA initial description",
                "content", "QA initial content", "image", "img/images.jpg"));
        assertRedirect(fixture.client.post("admin-add-news", fields), "admin-news");
        captureCreatedId(fixture);
        fields.put("id", fixture.id);
        fields.put("shortDesc", "QA updated description");
        fields.put("content", "QA updated content");
        assertRedirect(fixture.client.post("admin-news", fields), "admin-news");
        Response saved = fixture.client.get("admin-news?type=edit&id=" + fixture.id);
        assertEquals(200, saved.status());
        Document form = saved.document();
        assertEquals(fixture.marker, form.selectFirst("input[name=title]").val());
        assertEquals("QA updated description", form.selectFirst("textarea[name=shortDesc]").val());
        assertEquals("QA updated content", form.selectFirst("textarea[name=content]").val());
        assertEquals(List.of(fixture.id), ownedIds(fixture));
        deleteFixture(fixture);
    }

    @Test
    @DisplayName("ADM-CRUD-02 Admin tạo, đọc, sửa và xóa voucher")
    void voucherCreateUpdateDelete() throws Exception {
        Fixture fixture = registerFixture("admin-vouchers",
                "QA" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(java.util.Locale.ROOT));
        Map<String, String> fields = new HashMap<>(Map.of(
                "action", "insert", "code", fixture.marker, "description", "QA initial voucher",
                "discountAmount", "1000", "discountType", "FIXED", "minOrder", "20000",
                "expiryDate", LocalDate.now().plusDays(30).toString()));
        assertRedirect(fixture.client.post("admin-vouchers", fields), "admin-vouchers");
        captureCreatedId(fixture);
        fields.put("action", "update");
        fields.put("id", fixture.id);
        fields.put("description", "QA updated voucher");
        fields.put("discountAmount", "2000");
        assertRedirect(fixture.client.post("admin-vouchers", fields), "admin-vouchers");
        Response saved = fixture.client.get("admin-vouchers?action=edit&id=" + fixture.id);
        assertEquals(200, saved.status());
        Document form = saved.document();
        assertEquals(fixture.marker, form.selectFirst("input[name=code]").val());
        assertEquals("QA updated voucher", form.selectFirst("input[name=description]").val());
        assertEquals(2000.0, Double.parseDouble(form.selectFirst("input[name=discountAmount]").val()));
        assertEquals(List.of(fixture.id), ownedIds(fixture));
        deleteFixture(fixture);
    }
}
