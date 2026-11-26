package controller.admin;

import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.Order;

import java.io.IOException;

@WebServlet("/admin-invoice")
public class AdminInvoiceController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        OrderDAO dao = new OrderDAO();
        String raw = request.getParameter("id");
        if (raw == null) {
            request.setAttribute("invoiceOrders", dao.getAllOrders().stream().filter(Order::isSuccessStatus).toList());
        } else {
            int id;
            try {
                id = Integer.parseInt(raw);
            } catch (NumberFormatException error) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            Order order = id > 0 ? dao.getOrderById(id) : null;
            if (order == null || !order.isSuccessStatus()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đơn đã giao thành công.");
                return;
            }
            request.setAttribute("invoiceOrder", order);
            request.setAttribute("invoiceLines", dao.getDetails(id));
        }
        request.getRequestDispatcher("/WEB-INF/views/admin-invoice.jsp").forward(request, response);
    }
}
