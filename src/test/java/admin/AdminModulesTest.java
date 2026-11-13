package admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("admin")
@DisplayName("Admin — trang quản trị, tìm kiếm, bộ lọc và XLSX")
public class AdminModulesTest extends AdminTestSupport {
    private Document page(String path) throws Exception {
        Response response = session("admin").get(path);
        assertEquals(200, response.status(), path);
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("html"),
                "Expected an HTML page at " + path);
        return response.document();
    }

    private record Link(String path, Map<String, String> query, String text) {}

    private List<Link> links(Document page, String route) {
        return page.select("a[href]").stream().filter(element -> {
            String path = URI.create(element.attr("href")).getPath();
            return path != null && (path.equals(route) || path.endsWith("/" + route));
        }).map(element -> {
            URI uri = URI.create(element.attr("href"));
            String rawQuery = uri.getRawQuery();
            Map<String, String> query = new HashMap<>();
            if (rawQuery != null) {
                for (String pair : rawQuery.split("&")) {
                    String[] parts = pair.split("=", 2);
                    query.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
                }
            }
            return new Link(route + (rawQuery == null ? "" : "?" + rawQuery), query, element.text());
        }).toList();
    }

    private Link dataLink(Document page, String route, String key) {
        List<Link> candidates = links(page, route).stream()
                .filter(link -> link.query().containsKey(key)).toList();
        assumeTrue(!candidates.isEmpty(), "QA data needs a " + route + " link with " + key);
        return candidates.getFirst();
    }

    private void assertFields(Document page, String... names) {
        for (String name : names) {
            assertNotNull(page.selectFirst("[name=" + name + "]"), "Missing form field " + name);
        }
    }

    private String value(Document page, String name) {
        Element field = page.selectFirst("input[name=" + name + "]");
        assertNotNull(field, "Missing input " + name);
        return field.val();
    }

    private String selectedValue(Document page, String name) {
        Element option = page.selectFirst("select[name=" + name + "] option[selected]");
        assertNotNull(option, "Missing selected option for " + name);
        return option.val();
    }

    private Set<String> orderIds(Document page, String badge) {
        Set<String> ids = new HashSet<>();
        for (Element row : page.select("table.admin-table tbody tr")) {
            if (!row.select("." + badge).isEmpty()) {
                ids.add(row.selectFirst("td").text());
            }
        }
        return ids;
    }

    private void assertWorkbook(Response response, Set<String> expectedNames) throws Exception {
        assertEquals(200, response.status(), "Workbook download failed.");
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("spreadsheetml.sheet"));
        assertTrue(response.headers().firstValue("Content-Disposition").orElse("").contains("attachment"));
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response.body()))) {
            while (zip.getNextEntry() != null) {
                zip.readAllBytes(); // Reading each entry to the end also validates its CRC.
                entries++;
            }
        }
        assertTrue(entries > 0, "Download is not an XLSX ZIP container.");
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.body()))) {
            Set<String> names = new HashSet<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                names.add(workbook.getSheetName(index));
                assertTrue(workbook.getSheetAt(index).getPhysicalNumberOfRows() > 0,
                        "Exported worksheet is empty.");
            }
            assertEquals(expectedNames, names);
        }
    }

    @Test
    @DisplayName("ADM-MOD-01 Dashboard có thống kê và điều hướng")
    void dashboard() throws Exception {
        Document doc = page("admin");
        assertTrue(doc.text().contains("Tổng quan Dashboard"));
        assertTrue(doc.text().contains("Tổng đơn hàng"));
        assertTrue(doc.text().contains("Doanh thu thực"));
        assertFalse(links(doc, "admin-orders").isEmpty());
        assertFalse(links(doc, "admin-revenue-report").isEmpty());
    }

    @Test
    @DisplayName("ADM-MOD-02 Bộ lọc đơn hàng trả đúng nhóm trạng thái")
    void orderFilters() throws Exception {
        Document baseline = page("admin-orders");
        Map<String, String> filters = Map.of("processing", "bg-process", "shipping", "bg-shipping",
                "success", "bg-success", "cancel", "bg-cancel");
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            Document filtered = page("admin-orders?filter=" + filter.getKey());
            assertEquals(orderIds(baseline, filter.getValue()), orderIds(filtered, "badge"),
                    filter.getKey());
            assertEquals(filter.getKey(), selectedValue(filtered, "filter"));
        }
    }

    @Test
    @DisplayName("ADM-MOD-03 Tìm được sản phẩm lấy từ danh sách thật")
    void productSearch() throws Exception {
        Link sample = dataLink(page("admin-products"), "admin-variants", "pid");
        assertFalse(sample.text().isBlank());
        Document found = page("admin-products?search=" + URLEncoder.encode(sample.text(), StandardCharsets.UTF_8));
        List<Link> products = links(found, "admin-variants");
        assertFalse(products.isEmpty());
        assertTrue(products.stream().anyMatch(link -> sample.query().get("pid").equals(link.query().get("pid"))));
        assertTrue(products.stream().allMatch(link -> link.text().toLowerCase(Locale.ROOT)
                .contains(sample.text().toLowerCase(Locale.ROOT))), "Search returned an unrelated product.");
    }

    @Test
    @DisplayName("ADM-MOD-04 Từ khóa sản phẩm không tồn tại hiển thị rỗng")
    void emptyProductSearch() throws Exception {
        Document doc = page("admin-products?search=QA_MISSING_" + UUID.randomUUID());
        assertTrue(links(doc, "admin-variants").isEmpty());
        assertTrue(doc.text().contains("Chưa có sản phẩm nào"));
    }

    @Test
    @DisplayName("ADM-MOD-05 Form thêm và sửa sản phẩm có action và ID đúng")
    void productForms() throws Exception {
        Document add = page("admin-products?type=add");
        assertFields(add, "csrfToken", "action", "name", "price", "cateId", "stock", "image");
        assertEquals("add", value(add, "action"));
        Link sample = links(page("admin-products"), "admin-products").stream()
                .filter(link -> "edit".equals(link.query().get("type")) && link.query().containsKey("pid"))
                .findFirst().orElse(null);
        assumeTrue(sample != null, "QA data needs one product with an edit link.");
        Document edit = page(sample.path());
        assertFields(edit, "csrfToken", "action", "name", "price", "cateId", "stock", "image", "pid");
        assertEquals("update", value(edit, "action"));
        assertEquals(sample.query().get("pid"), value(edit, "pid"));
        assertFalse(value(edit, "name").isBlank());
    }

    @Test
    @DisplayName("ADM-MOD-06 Biến thể thuộc đúng sản phẩm đã chọn")
    void variantPage() throws Exception {
        Link sample = dataLink(page("admin-products"), "admin-variants", "pid");
        Document doc = page(sample.path());
        assertTrue(doc.text().contains(sample.text()));
        assertFalse(links(doc, "admin-products").isEmpty());
        if (!doc.select("input[name=vid]").isEmpty()) {
            assertFields(doc, "csrfToken", "action", "vid", "pid", "newQty");
            for (Element id : doc.select("input[name=pid]")) {
                assertEquals(sample.query().get("pid"), id.val());
            }
        }
    }

    @Test
    @DisplayName("ADM-MOD-07 ID biến thể thiếu hoặc sai được xử lý")
    void invalidVariantProductId() throws Exception {
        for (String path : List.of("admin-variants", "admin-variants?pid=bad-id", "admin-variants?pid=0")) {
            assertRedirect(session("admin").get(path), "admin-products");
        }
    }

    @Test
    @DisplayName("ADM-MOD-08 Danh sách và tìm kiếm khách hàng rỗng")
    void customerSearch() throws Exception {
        Document listing = page("admin-users");
        assertTrue(listing.text().contains("Quản Lý Khách Hàng"));
        assertFields(listing, "search");
        Document empty = page("admin-users?search=QA_MISSING_" + UUID.randomUUID());
        assertTrue(links(empty, "admin-user-detail").isEmpty());
        assertTrue(empty.text().contains("Không tìm thấy khách hàng"));
    }

    @Test
    @DisplayName("ADM-MOD-09 Chi tiết đúng khách hàng đã chọn")
    void customerDetail() throws Exception {
        Link sample = dataLink(page("admin-users"), "admin-user-detail", "uid");
        Document detail = page(sample.path());
        assertTrue(detail.text().contains("Thông Tin Cá Nhân"));
        assertTrue(detail.text().contains("Lịch Sử Đơn Hàng"));
        assertTrue(detail.text().contains(sample.text()));
        assertFalse(links(detail, "admin-users").isEmpty());
    }

    @Test
    @DisplayName("ADM-MOD-10 ID khách hàng thiếu hoặc sai được xử lý")
    void invalidCustomerId() throws Exception {
        for (String path : List.of("admin-user-detail", "admin-user-detail?uid=bad-id", "admin-user-detail?uid=0")) {
            assertRedirect(session("admin").get(path), "admin-users");
        }
    }

    @Test
    @DisplayName("ADM-MOD-11 Danh sách và form tạo nhân viên")
    void staffPages() throws Exception {
        Document listing = page("admin-staffs");
        assertTrue(listing.text().contains("Quản Lý Nhân Viên"));
        assertFields(listing, "search");
        assertFields(page("admin-add-staff"), "csrfToken", "username", "password", "fullname", "email", "phone");
    }

    @Test
    @DisplayName("ADM-MOD-12 Form và lịch sử nhập kho")
    void stockImportPage() throws Exception {
        Document doc = page("admin-stock-import");
        assertTrue(doc.text().contains("Lập phiếu nhập kho"));
        assertTrue(doc.text().contains("Phiếu nhập gần đây"));
        assumeTrue(doc.selectFirst("[name=productId]") != null, "QA data needs at least one product.");
        assertFields(doc, "csrfToken", "importDate", "supplier", "note", "productId",
                "sizes", "colors", "quantity", "unitCost");
    }

    @Test
    @DisplayName("ADM-MOD-13 Danh sách và form thêm, sửa tin tức")
    void newsPages() throws Exception {
        Document listing = page("admin-news");
        assertTrue(listing.text().contains("Quản Lý Tin Tức"));
        assertFields(page("admin-add-news.jsp"), "csrfToken", "title", "shortDesc", "content", "image");
        Link sample = dataLink(listing, "admin-news", "id");
        Document edit = page(sample.path());
        assertFields(edit, "csrfToken", "title", "shortDesc", "content", "image", "id");
        assertEquals(sample.query().get("id"), value(edit, "id"));
    }

    @Test
    @DisplayName("ADM-MOD-14 Form voucher có action và ID đúng")
    void voucherPages() throws Exception {
        Document listing = page("admin-vouchers");
        assertTrue(listing.text().contains("Quản lý Mã Giảm Giá"));
        Document add = page("admin-vouchers?action=add");
        assertFields(add, "csrfToken", "action", "code", "description", "discountType",
                "discountAmount", "minOrder", "expiryDate");
        assertEquals("insert", value(add, "action"));
        Link sample = dataLink(listing, "admin-vouchers", "id");
        Document edit = page(sample.path());
        assertFields(edit, "csrfToken", "action", "code", "description", "discountType",
                "discountAmount", "minOrder", "expiryDate", "id");
        assertEquals(sample.query().get("id"), value(edit, "id"));
        assertEquals("update", value(edit, "action"));
    }

    @Test
    @DisplayName("ADM-MOD-15 Bộ lọc báo cáo doanh thu đúng nhóm")
    void revenueFilters() throws Exception {
        for (String selected : List.of("sold", "unsold")) {
            Document doc = page("admin-revenue-report?filter=" + selected);
            assertFields(doc, "period", "month", "year", "filter", "q", "scope");
            assertEquals(selected, selectedValue(doc, "filter"));
            String forbidden = "sold".equals(selected) ? "is-unsold" : "is-sold";
            assertTrue(doc.select("table.revenue-table ." + forbidden).isEmpty());
            assertFalse(doc.text().contains("Không thể tải báo cáo lúc này"));
        }
    }

    @Test
    @DisplayName("ADM-MOD-16 Xuất doanh thu XLSX đúng cấu trúc và kỳ")
    void revenueWorkbook() throws Exception {
        Document doc = page("admin-revenue-report");
        Element form = doc.selectFirst("form[action=admin-export-revenue]");
        assertNotNull(form, "Export form is missing.");
        StringBuilder path = new StringBuilder("admin-export-revenue?scope=all");
        for (String name : List.of("period", "month", "year")) {
            Element input = form.selectFirst("input[name=" + name + "]");
            assertNotNull(input, "Missing export period field " + name);
            path.append('&').append(name).append('=')
                    .append(URLEncoder.encode(input.val(), StandardCharsets.UTF_8));
        }
        assertWorkbook(session("admin").get(path.toString()), Set.of("Tổng quan", "Chi tiết mặt hàng"));
    }

    @Test
    @DisplayName("ADM-MOD-17 Xuất XLSX phiếu nhập lấy từ lịch sử")
    void stockReceiptWorkbook() throws Exception {
        Link sample = dataLink(page("admin-stock-import"), "admin-stock-import/export", "receiptId");
        assertWorkbook(session("admin").get(sample.path()), Set.of("Phiếu nhập kho"));
    }

    @Test
    @DisplayName("ADM-MOD-18 ID phiếu nhập sai trả lỗi có kiểm soát")
    void invalidStockReceiptId() throws Exception {
        Map<String, Integer> expected = Map.of("", 400, "?receiptId=bad-id", 400, "?receiptId=0", 404);
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            Response response = session("admin").get("admin-stock-import/export" + entry.getKey());
            assertEquals(entry.getValue().intValue(), response.status(), entry.getKey());
            assertFalse(response.body().length > 1 && response.body()[0] == 'P' && response.body()[1] == 'K');
        }
    }
}
