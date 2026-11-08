package controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;

import java.io.IOException;
import java.util.List;

import dao.UserDAO;

@WebServlet("/admin-staffs")
public class AdminStaffController extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        UserDAO dao = new UserDAO();
        String type = request.getParameter("type");
        String keyword = request.getParameter("search");

        List<User> list;
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = dao.searchUsers(keyword.trim());
            request.setAttribute("searchKeyword", keyword.trim());
        } else {
            list = dao.getAllUsers();
        }
        if (list != null) {
            list.removeIf(u -> u.getIsAdmin() != 2);
        }
        request.setAttribute("listStaffs", list);
        request.getRequestDispatcher("/admin-staffs.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        UserDAO dao = new UserDAO();
        String type = request.getParameter("type");

        if ("delete".equals(type)) {
            int uid = Integer.parseInt(request.getParameter("uid"));
            dao.deleteUser(uid);
            response.sendRedirect("admin-staffs?msg=deleted");
            return;
        }

        if ("changepass".equals(type)) {
            String uname = request.getParameter("uname");
            String newPass = util.MD5.getMd5(request.getParameter("newpass"));
            dao.changePassword(uname, newPass);
            response.sendRedirect("admin-staffs?msg=pass_changed");
            return;
        }

        response.sendRedirect("admin-staffs");
    }
}
