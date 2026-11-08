package filter;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

/** Protects the administrative customer/order views before a DAO or JSP runs. */
@WebFilter(filterName = "AdminReadAccessFilter", urlPatterns = {
        "/admin-users", "/admin-user-detail", "/admin-orders",
        "/admin-users.jsp", "/admin-user-detail.jsp", "/admin-orders.jsp"
}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE})
public class AdminReadAccessFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        Object account = session == null ? null : session.getAttribute("user");
        User user = account instanceof User ? (User) account : null;
        boolean admin = user != null && user.getIsAdmin() == 1;
        boolean staffRead = user != null && user.getIsAdmin() == 2
                && ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod()));
        // isAdmin is also set for staff at login; it is not an authoritative role.
        if (!admin && !staffRead) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
