package controller.admin;

import java.io.IOException;
import java.util.List;
import dao.ProductDAO;
import model.Product;
import model.ProductVariant;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.InputValidator;

@WebServlet("/admin-variants")
public class AdminVariantController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            // Lấy ID sản phẩm từ URL
            int pid = Integer.parseInt(request.getParameter("pid"));
            ProductDAO dao = new ProductDAO();

            // Lấy thông tin sản phẩm
            Product p = dao.getProductById(pid);

            if (p != null) {
                // Lấy danh sách biến thể (màu/size) của sản phẩm này
                List<ProductVariant> variants = dao.getVariantsByProductId(pid);
                p.setVariants(variants);

                // Truyền dữ liệu sang giao diện mới
                request.setAttribute("product", p);
                request.setAttribute("variants", variants);
                request.getRequestDispatcher("/admin-variants.jsp").forward(request, response);
            } else {
                response.sendRedirect("admin-products?msg=error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("admin-products?msg=error");
        }
    }
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        int pid;
        try {
            pid = Integer.parseInt(request.getParameter("pid"));
            if (pid <= 0) throw new IllegalArgumentException("Invalid product ID.");
        } catch (IllegalArgumentException error) {
            response.sendRedirect("admin-products?msg=error_data");
            return;
        }
        ProductDAO dao = new ProductDAO();

        try {
            if ("update-qty".equals(action)) {
                int vid = Integer.parseInt(request.getParameter("vid"));
                if (vid <= 0) throw new IllegalArgumentException("Invalid variant ID.");
                int newQty = InputValidator.productStock(request.getParameter("newQty"));
                boolean updated = dao.updateVariantQuantity(vid, newQty);
                response.sendRedirect("admin-variants?pid=" + pid + "&msg="
                        + (updated ? "success" : "error"));
                return;
            } else if ("delete-variant".equals(action)) {
                int vid = Integer.parseInt(request.getParameter("vid"));
                dao.deleteVariant(vid);
            } else {
                response.sendRedirect("admin-variants?pid=" + pid + "&msg=error_data");
                return;
            }
            // Sau khi xử lý xong, quay lại đúng trang chi tiết của sản phẩm đó
            response.sendRedirect("admin-variants?pid=" + pid + "&msg=success");
        } catch (IllegalArgumentException error) {
            response.sendRedirect("admin-variants?pid=" + pid + "&msg=error_data");
        } catch (Exception e) {
            getServletContext().log("Unable to update Admin variant stock", e);
            response.sendRedirect("admin-variants?pid=" + pid + "&msg=error");
        }
    }
}
