package admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("admin")
@DisplayName("Admin — đăng nhập, phân quyền và CSRF")
public class AdminAccessTest extends AdminTestSupport {
    private static final List<String> PROTECTED_PATHS = List.of(
            "admin", "admin-orders", "admin-products", "admin-users",
            "admin-user-detail?uid=-1", "admin-staffs", "admin-add-staff",
            "admin-news", "admin-vouchers", "admin-collections",
            "admin-revenue-report", "admin-export-report", "admin-export-revenue",
            "admin-stock-import", "admin-stock-import/export?receiptId=-1");

    @Test
    @DisplayName("ADM-AUTH-01 Đăng nhập đúng theo từng vai trò")
    void successfulRoleLogin() throws Exception {
        for (String role : List.of("admin", "staff", "customer")) {
            AdminClient client = new AdminClient();
            Credentials account = credentials(role);
            Response response = client.login(account.username(), account.password());
            assertSuccessfulLogin(response, role, account.username());
            if ("customer".equals(role)) {
                assertDenied(client.get("admin"));
            } else {
                assertEquals(200, client.get("admin").status(), role + " dashboard");
            }
        }
    }

    @Test
    @DisplayName("ADM-AUTH-02 Mật khẩu sai không cấp quyền")
    void wrongPasswordRemainsUnauthorized() throws Exception {
        Credentials account = credentials("admin");
        AdminClient client = new AdminClient();
        Response response = client.login(account.username(), account.password() + "_invalid");
        assertEquals(200, response.status());
        assertTrue(response.text().contains("Sai tài khoản hoặc mật khẩu"), "Missing login error.");
        assertDenied(client.get("admin"));
        assertDenied(client.get("admin-products"));
    }

    @Test
    @DisplayName("ADM-AUTH-03 Đăng xuất thu hồi quyền đọc và export")
    void logoutRevokesAccess() throws Exception {
        AdminClient client = session("admin");
        assertEquals(200, client.get("admin-orders").status());
        assertRedirect(client.get("logout"), "home");
        for (String path : List.of("admin", "admin-orders", "admin-users", "admin-export-report")) {
            assertAll(path, () -> assertDenied(client.get(path)));
        }
    }

    @Test
    @DisplayName("ADM-AUTH-04 Guest bị chặn tại các route Admin")
    void guestRouteMatrix() throws Exception {
        AdminClient client = session("guest");
        for (String path : PROTECTED_PATHS) {
            assertAll(path, () -> assertDenied(client.get(path)));
        }
    }

    @Test
    @DisplayName("ADM-AUTH-05 Customer bị chặn tại các route Admin")
    void customerRouteMatrix() throws Exception {
        AdminClient client = session("customer");
        for (String path : PROTECTED_PATHS) {
            assertAll(path, () -> assertDenied(client.get(path)));
        }
    }

    @Test
    @DisplayName("ADM-AUTH-06 Staff mở được các trang được phép")
    void staffAllowedPages() throws Exception {
        AdminClient client = session("staff");
        for (String path : List.of("admin", "admin-orders", "admin-products", "admin-stock-import")) {
            assertEquals(200, client.get(path).status(), path);
        }
    }

    @Test
    @DisplayName("ADM-AUTH-07 Staff bị chặn tại module chỉ dành cho Admin")
    void staffAdminOnlyRouteMatrix() throws Exception {
        AdminClient client = session("staff");
        for (String path : List.of(
                "admin-users", "admin-user-detail?uid=-1", "admin-staffs", "admin-add-staff",
                "admin-news", "admin-add-news.jsp", "admin-vouchers", "admin-collections",
                "admin-revenue-report", "admin-export-report", "admin-export-revenue",
                "admin-stock-import/export?receiptId=-1")) {
            assertAll(path, () -> assertDenied(client.get(path)));
        }
    }

    @Test
    @DisplayName("ADM-AUTH-08 Truy cập trực tiếp JSP vẫn kiểm tra quyền")
    void directJspRequestsAreProtected() throws Exception {
        for (String role : List.of("guest", "customer")) {
            AdminClient client = session(role);
            for (String path : List.of(
                    "admin.jsp", "admin-orders.jsp", "admin-users.jsp", "admin-user-detail.jsp",
                    "admin-products.jsp", "admin-product-form.jsp", "admin-add-product.jsp",
                    "admin-variants.jsp", "admin-add-news.jsp", "admin-vouchers.jsp")) {
                assertAll(role + " " + path, () -> assertDenied(client.get(path)));
            }
        }
    }

    @Test
    @DisplayName("ADM-AUTH-09 CSRF đúng vẫn phải kiểm tra vai trò khi ghi")
    void validCsrfDoesNotGrantWritePermission() throws Exception {
        Map<String, Map<String, String>> requests = Map.of(
                "admin-products", Map.of("action", "update", "pid", "invalid", "cateId", "invalid"),
                "admin-add-product", Map.of("price", "invalid", "cateId", "invalid", "stock", "invalid"),
                "admin-variants", Map.of("action", "update-qty", "pid", "invalid", "vid", "invalid"),
                "admin-news", Map.of("id", "invalid", "type", "delete"),
                "admin-vouchers", Map.of("action", "delete", "id", "invalid"));
        for (String role : List.of("guest", "customer")) {
            AdminClient client = session(role);
            for (Map.Entry<String, Map<String, String>> entry : requests.entrySet()) {
                assertAll(role + " " + entry.getKey(),
                        () -> assertDenied(client.post(entry.getKey(), entry.getValue())));
            }
        }
    }

    @Test
    @DisplayName("ADM-AUTH-10 Staff không được xóa sản phẩm")
    void staffCannotDeleteProducts() throws Exception {
        Response response = session("staff").post("admin-products",
                Map.of("action", "delete", "pid", "invalid"));
        assertRedirect(response, "admin-products");
        assertTrue(response.location().contains("msg=error_permission"), "Missing permission error.");
    }

    @Test
    @DisplayName("ADM-AUTH-11 CSRF thiếu hoặc sai bị chặn, mã đúng được xử lý")
    void csrfValidationWithPositiveControl() throws Exception {
        AdminClient client = session("admin");
        Map<String, String> fields = Map.of("action", "import", "pid", "0");
        assertEquals(403, client.postWithoutCsrf("admin-products", fields).status());
        assertEquals(403, client.postWithToken("admin-products", fields, "invalid-csrf-token").status());
        Response positive = client.post("admin-products", fields);
        assertRedirect(positive, "admin-stock-import");
        assertTrue(positive.location().contains("msg=invalid"), "Valid CSRF did not reach the controller.");
    }

    @Test
    @DisplayName("ADM-AUTH-12 Path parameter không vượt qua chặn quyền")
    void pathParametersCannotBypassAuthorization() throws Exception {
        String path = "admin-products;qa=probe";
        assertEquals(200, session("admin").get(path).status(), "Admin positive control.");
        assertDenied(session("guest").get(path));
        assertDenied(session("customer").get(path));
    }
}
