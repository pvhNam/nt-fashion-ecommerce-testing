package controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/search")
public class SearchController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Ô tìm kiếm trên header dùng chung trang Sản phẩm (/about) để có lọc giá, sắp xếp và phân trang
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String txtSearch = request.getParameter("txt");
        if (txtSearch != null) txtSearch = txtSearch.trim();

        String target = request.getContextPath() + "/about";
        if (txtSearch != null && !txtSearch.isEmpty()) {
            target += "?txt=" + URLEncoder.encode(txtSearch, StandardCharsets.UTF_8);
        }
        response.sendRedirect(target);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
