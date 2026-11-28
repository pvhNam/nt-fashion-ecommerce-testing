package controller.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import dao.CategoryDAO;
import dao.ProductDAO;
import model.Product;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.AdminAccess;
import util.InputValidator;

@WebServlet("/admin-products")
public class AdminProductController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String type = request.getParameter("type");
        String keyword = request.getParameter("search");
        ProductDAO dao = new ProductDAO();

        if ("add".equals(type)) {
            showProductForm(request, response);
            return;
        }

        if ("edit".equals(type)) {
            int pid;
            try {
                pid = Integer.parseInt(request.getParameter("pid"));
            } catch (NumberFormatException e) {
                response.sendRedirect("admin-products?msg=error");
                return;
            }
            request.setAttribute("product", dao.getProductById(pid));
            showProductForm(request, response);
            return;
        }

        if ("import".equals(type)) {
            try {
                int pid = Integer.parseInt(request.getParameter("pid"));
                response.sendRedirect(request.getContextPath() + "/admin-stock-import?pid=" + pid);
            } catch (Exception e) {
                response.sendRedirect("admin-products?msg=error");
            }
            return;
        }

        List<Product> list;
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = dao.searchProduct(keyword.trim());
            request.setAttribute("searchKeyword", keyword);
        } else {
            list = dao.getAllProducts();
        }

        list = AdminPagination.page(request, list, "search");
        request.setAttribute("listProducts", list);
        request.setAttribute("listP", list);
        request.getRequestDispatcher("/admin-products.jsp").forward(request, response);
    }

    private void showProductForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadCategories(request, response, getServletContext());
        request.getRequestDispatcher("/admin-product-form.jsp").forward(request, response);
    }

    static void loadCategories(HttpServletRequest request, HttpServletResponse response,
            ServletContext context) {
        try {
            request.setAttribute("categories", new CategoryDAO().findAll());
        } catch (SQLException error) {
            context.log("Unable to load product categories", error);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("categoriesUnavailable", true);
            request.setAttribute("categories", List.of());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        ProductDAO dao = new ProductDAO();

        if ("delete".equals(action)) {
            if (!AdminAccess.isAdmin(request)) {
                response.sendRedirect("admin-products?msg=error_permission");
                return;
            }
            int pid;
            try {
                pid = Integer.parseInt(request.getParameter("pid"));
            } catch (NumberFormatException e) {
                response.sendRedirect("admin-products?msg=error_delete_id");
                return;
            }
            if (pid <= 0) {
                response.sendRedirect("admin-products?msg=error_delete_id");
                return;
            }
            String message = switch (dao.deleteProduct(pid)) {
                case DELETED -> "deleted";
                case NOT_FOUND -> "error_delete_not_found";
                case REFERENCED -> "error_delete_referenced";
                case FAILED -> "error_delete";
            };
            response.sendRedirect("admin-products?msg=" + message);
            return;
        }

        try {
            if ("import".equals(action)) {
                int pid = Integer.parseInt(request.getParameter("pid"));
                response.sendRedirect(request.getContextPath()
                        + "/admin-stock-import?pid=" + pid + "&msg=invalid");
                return;
            }

            if (!"add".equals(action) && !"update".equals(action)) {
                response.sendRedirect("admin-products?msg=error_data");
                return;
            }

            String name = request.getParameter("name");
            double price = InputValidator.productPrice(request.getParameter("price"));
            int cateId = Integer.parseInt(request.getParameter("cateId"));
            String color = request.getParameter("color");
            String size = request.getParameter("size");
            String image = request.getParameter("image");
            int stock = InputValidator.productStock(request.getParameter("stock"));

            String[] extraImages = request.getParameterValues("extraImages");
            if (!validImages(image, extraImages)) {
                response.sendRedirect("admin-products?msg=error_image");
                return;
            }
            if (!new CategoryDAO().exists(cateId)) {
                response.sendRedirect("admin-products?msg=error_category");
                return;
            }

            if ("add".equals(action)) {
                Product p = new Product(0, name, price, cateId, color, size, stock, image);

                int newPid = dao.addProductWithImages(p, extraImages);

                if (newPid > 0) {
                    response.sendRedirect("admin-products?msg=added");
                } else {
                    response.sendRedirect("admin-products?msg=error_add");
                }

            } else if ("update".equals(action)) {
                int pid = Integer.parseInt(request.getParameter("pid"));
                Product p = new Product(pid, name, price, cateId, color, size, stock, image);

                boolean updated = dao.updateProductWithImages(p, extraImages);
                response.sendRedirect("admin-products?msg=" + (updated ? "updated" : "error_update"));
            }
        } catch (IllegalArgumentException e) {
            response.sendRedirect("admin-products?msg=error_data");
        } catch (Exception e) {
            getServletContext().log("Unable to save Admin product", e);
            response.sendRedirect("admin-products?msg=error_data");
        }
    }

    private boolean validImages(String image, String[] extraImages) {
        if (!InputValidator.isSafeImageUrl(image)) return false;
        if (extraImages == null) return true;
        for (String url : extraImages) {
            if (url != null && !url.isBlank() && !InputValidator.isSafeImageUrl(url)) return false;
        }
        return true;
    }
}
