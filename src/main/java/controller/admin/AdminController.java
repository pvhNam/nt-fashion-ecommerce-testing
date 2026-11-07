package controller.admin;

import java.io.IOException;
import java.util.List;

import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Order;

@WebServlet("/admin")
public class AdminController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        OrderDAO dao = new OrderDAO();
        List<Order> list = dao.getAllOrders();

        double totalRevenue = 0;
        int countSuccess = 0;
        int countShipping = 0;
        int countProcessing = 0;
        int countCancel = 0;

        if (list != null) {
            for (Order order : list) {
                if (order.isSuccessStatus()) {
                    totalRevenue += order.getTotalMoney();
                    countSuccess++;
                } else if (order.isShippingStatus()) {
                    countShipping++;
                } else if (order.isCancelledStatus()) {
                    countCancel++;
                } else {
                    countProcessing++;
                }
            }
        }

        request.setAttribute("listOrders", list);
        request.setAttribute("totalOrders", list != null ? list.size() : 0);
        request.setAttribute("totalRevenue", totalRevenue);
        request.setAttribute("countSuccess", countSuccess);
        request.setAttribute("countShipping", countShipping);
        request.setAttribute("countProcessing", countProcessing);
        request.setAttribute("countCancel", countCancel);
        request.getRequestDispatcher("/admin.jsp").forward(request, response);
    }
}
