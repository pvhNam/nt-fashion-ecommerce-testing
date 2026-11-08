package filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Mã chống giả mạo (CSRF) cho mọi yêu cầu POST (KAN-11: W1-04).
// Mỗi phiên có một mã, lưu ở session với tên csrfToken; trang JSP gắn vào form bằng ${csrfToken},
// lệnh fetch gửi qua header X-CSRF-Token. Sai hoặc thiếu mã → 403.
@WebFilter("/*")
public class CsrfFilter implements Filter {
    public static final String TOKEN = "csrfToken";
    private static final String HEADER = "X-CSRF-Token";
    // Cổng thanh toán gọi thẳng vào server, không đi qua form của trang
    private static final Set<String> EXEMPT_PATHS = Set.of("/momo-callback", "/vnpay-ipn");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getServletPath();

        if (isStaticResource(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(TOKEN);
        if (token == null) {
            token = newToken();
            session.setAttribute(TOKEN, token);
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) && !EXEMPT_PATHS.contains(path)) {
            // Đọc tham số ở đây sẽ khóa bảng mã, nên đặt UTF-8 trước như các controller
            if (request.getCharacterEncoding() == null) {
                request.setCharacterEncoding("UTF-8");
            }
            String submitted = request.getHeader(HEADER);
            if (submitted == null || submitted.isEmpty()) {
                submitted = request.getParameter(TOKEN);
            }
            if (!matches(token, submitted)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Phiên làm việc không hợp lệ hoặc đã hết hạn. Vui lòng tải lại trang và thử lại.");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private boolean isStaticResource(String path) {
        return path.startsWith("/CSS/") || path.startsWith("/JS/") || path.startsWith("/img/")
                || path.matches(".*\\.(css|js|png|jpe?g|gif|svg|webp|ico|woff2?|ttf|map)$");
    }

    private boolean matches(String expected, String submitted) {
        return submitted != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
