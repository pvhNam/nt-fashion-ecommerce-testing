package controller.admin;

import dao.UserDAO;
import model.User;
import util.MD5;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin-add-staff")
public class AdminAddStaffController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/admin-add-staff.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String fullname = request.getParameter("fullname");
        String phone = request.getParameter("phone");

        UserDAO dao = new UserDAO();
        User existingUser = dao.checkEmailExist(email);

        if (existingUser != null) {
            request.setAttribute("error", "Lỗi: Email hoặc Tài khoản này đã tồn tại trong hệ thống!");
            request.getRequestDispatcher("/admin-add-staff.jsp").forward(request, response);
            return;
        }

        String hashedPass = MD5.getMd5(password);
        boolean isSuccess = dao.registerStaff(username, hashedPass, email, fullname, phone);

        if (isSuccess) {
            response.sendRedirect("admin-staffs?msg=staff_added");
        } else {
            request.setAttribute("error", "Lỗi hệ thống!");
            request.getRequestDispatcher("/admin-add-staff.jsp").forward(request, response);
        }
    }
}
