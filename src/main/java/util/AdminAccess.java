package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.User;

// Vai trò lấy từ cột users.is_admin: 0 = khách hàng, 1 = Admin, 2 = Staff
public final class AdminAccess {
    public static final int CUSTOMER = 0;
    public static final int ADMIN = 1;
    public static final int STAFF = 2;

    private AdminAccess() {}

    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    public static int role(HttpServletRequest request) {
        User user = currentUser(request);
        return user == null ? CUSTOMER : user.getIsAdmin();
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return role(request) == ADMIN;
    }

    public static boolean isAdminOrStaff(HttpServletRequest request) {
        int role = role(request);
        return role == ADMIN || role == STAFF;
    }
}
