package controller.user;

import java.io.IOException;
import dao.UserDAO;
import util.InputValidator;
import util.MD5;
import util.PasswordPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/register")
public class RegisterController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String username = InputValidator.trim(request.getParameter("user"));
        String pass = request.getParameter("pass");
        String re_pass = request.getParameter("re_pass");
        String email = InputValidator.trim(request.getParameter("email"));
        String fullname = InputValidator.trim(request.getParameter("fullname"));
        String rawPhone = InputValidator.trim(request.getParameter("phone"));
        String phone = InputValidator.normalizePhone(rawPhone);

        // Kiểm tra ở server, không chỉ dựa vào thuộc tính required của form (ST-14)
        String error = null;
        UserDAO dao = new UserDAO();
        if (fullname.isEmpty() || username.isEmpty() || email.isEmpty() || rawPhone.isEmpty()
                || pass == null || pass.isEmpty()) {
            error = "Vui lòng nhập đầy đủ họ tên, tên đăng nhập, email, số điện thoại và mật khẩu.";
        } else if (fullname.length() > 100 || username.length() > 50) {
            error = "Họ tên tối đa 100 ký tự, tên đăng nhập tối đa 50 ký tự.";
        } else if (!InputValidator.isValidEmail(email)) {
            error = "Email không hợp lệ.";
        } else if (phone == null) {
            error = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.";
        } else if (!pass.equals(re_pass)) {
            error = "Mật khẩu nhập lại không khớp.";
        } else if (!PasswordPolicy.isValid(pass)) {
            error = PasswordPolicy.MESSAGE;
        } else if (dao.checkUserExist(username) != null) {
            error = "Tên đăng nhập đã tồn tại.";
        } else if (dao.checkEmailExist(email) != null) {
            // IS-12: không cho hai tài khoản trùng email
            error = "Email này đã được dùng cho một tài khoản khác.";
        }

        if (error != null) {
            request.setAttribute("mess", error);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }

        dao.register(username, MD5.getMd5(pass), email, fullname, phone);
        response.sendRedirect("login");
    }
}
