package controller.admin;

import java.io.IOException;
import java.util.List;
import dao.NewsDAO;
import model.News;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin-news")
public class AdminNewsController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        NewsDAO dao = new NewsDAO();

        if ("edit".equals(type)) {
            int id;
            try {
                id = Integer.parseInt(request.getParameter("id"));
            } catch (NumberFormatException e) {
                response.sendRedirect("admin-news?msg=error_edit_id");
                return;
            }
            if (id <= 0) {
                response.sendRedirect("admin-news?msg=error_edit_id");
                return;
            }
            News n = dao.getNewsById(id);
            if (n == null) {
                response.sendRedirect("admin-news?msg=error_edit_not_found");
                return;
            }
            request.setAttribute("n", n);
            request.getRequestDispatcher("/admin-edit-news.jsp").forward(request, response);
        } else {
            List<News> list = dao.getAllNews();
            request.setAttribute("listN", AdminPagination.page(request, list));
            request.getRequestDispatcher("/admin-news.jsp").forward(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        int id = Integer.parseInt(request.getParameter("id"));
        if ("delete".equals(request.getParameter("type"))) {
            new NewsDAO().deleteNews(id);
            response.sendRedirect("admin-news");
            return;
        }

        String title = request.getParameter("title");
        String shortDesc = request.getParameter("shortDesc");
        String content = request.getParameter("content");
        String image = request.getParameter("image");

        News n = new News(id, title, shortDesc, content, image, null);
        NewsDAO dao = new NewsDAO();
        dao.updateNews(n);

        response.sendRedirect("admin-news");
    }
}
