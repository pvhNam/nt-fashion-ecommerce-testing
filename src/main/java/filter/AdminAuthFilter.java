package filter;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.AdminAccess;

// Kiểm tra quyền cho mọi trang quản trị (KAN-11: ST-10 / IS-02).
// Chưa đăng nhập → về trang đăng nhập; khách hàng → 403;
// Staff chỉ vào được đơn hàng, sản phẩm, phân loại, tồn kho; còn lại chỉ Admin.
@WebFilter("/*")
public class AdminAuthFilter implements Filter {
    private static final Set<String> STAFF_PATHS = Set.of(
            "/admin", "/admin-orders", "/update-order",
            "/admin-products", "/admin-add-product", "/admin-variants",
            "/admin-stock-import");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getServletPath();

        if (!isAdminPath(path)) {
            chain.doFilter(req, res);
            return;
        }

        if (AdminAccess.currentUser(request) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int role = AdminAccess.role(request);
        boolean allowed = role == AdminAccess.ADMIN
                || (role == AdminAccess.STAFF && STAFF_PATHS.contains(path));
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin") || path.equals("/update-order");
    }
}
