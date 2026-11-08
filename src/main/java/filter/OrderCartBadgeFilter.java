package filter;

import java.io.IOException;

import dao.CartDAO;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.User;

/** Refreshes the browser's badge after checkout or an asynchronous payment. */
@WebFilter(filterName = "OrderCartBadgeFilter", urlPatterns = {
        "/order-success", "/order-detail", "/order-success.jsp", "/order-detail.jsp"
})
public class OrderCartBadgeFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest) request).getSession(false);
        Object account = session == null ? null : session.getAttribute("user");
        if (account instanceof User user) {
            // Read the current cart instead of blindly setting zero: items added
            // after payment must remain visible, including in another browser tab.
            session.setAttribute("cartCount", new CartDAO().getCartByUid(user.getUid()).size());
        }
        chain.doFilter(request, response);
    }
}
