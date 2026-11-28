package controller.admin;

import java.io.IOException;
import dao.CategoryDAO;
import dao.ProductDAO;
import model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.InputValidator;

@WebServlet("/admin-add-product")
public class AdminAddProductController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AdminProductController.loadCategories(request, response, getServletContext());
        request.getRequestDispatcher("/admin-add-product.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        try {
            String name = request.getParameter("name");
            double price = InputValidator.productPrice(request.getParameter("price"));

            int cateId = Integer.parseInt(request.getParameter("cateId"));
            String color = request.getParameter("color");
            String size = request.getParameter("size");
            int stock = InputValidator.productStock(request.getParameter("stock"));
            String image = request.getParameter("image");
            if (!InputValidator.isSafeImageUrl(image)) {
                response.sendRedirect("admin-products?msg=error_image");
                return;
            }
            if (!new CategoryDAO().exists(cateId)) {
                response.sendRedirect("admin-products?msg=error_category");
                return;
            }

            Product p = new Product(0, name, price, cateId, color, size, stock, image);
            ProductDAO dao = new ProductDAO();

            boolean isAdded = dao.addProductWithImages(p, null) > 0;

            if (isAdded) {
                response.sendRedirect("admin-products?msg=added_product");
            } else {
                response.sendRedirect("admin-products?msg=error_add");
            }

        } catch (IllegalArgumentException e) {
            response.sendRedirect("admin-products?msg=error_data");
        } catch (Exception e) {
            getServletContext().log("Unable to add Admin product", e);
            response.sendRedirect("admin-products?msg=error_data");
        }
    }
}
