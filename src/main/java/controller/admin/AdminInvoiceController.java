package controller.admin;

import dao.InvoiceDAO;
import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Order;
import model.OrderDetail;
import model.User;
import service.InvoiceEmailService;
import util.AdminAccess;
import util.EmailUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet({"/admin-invoice", "/order-invoice"})
public class AdminInvoiceController extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/admin-invoice.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        boolean admin = "/admin-invoice".equals(request.getServletPath());
        User user = AdminAccess.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.setAttribute("invoiceAdmin", admin);
        OrderDAO dao = new OrderDAO();
        if (admin && request.getParameter("id") == null) {
            request.setAttribute("invoiceOrders", dao.getAllOrders().stream().filter(Order::isSuccessStatus).toList());
        } else {
            try {
                Order order = dao.getOrderById(parseId(request));
                if (order == null || !order.isSuccessStatus() || (!admin && order.getUserId() != user.getUid())) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                InvoiceDAO.Buyer buyer = new InvoiceDAO().find(order.getId());
                if (!admin && buyer == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cửa hàng chưa gửi hóa đơn cho đơn này.");
                    return;
                }
                request.setAttribute("invoiceBuyer", buyer);
                request.setAttribute("invoiceOrder", order);
                request.setAttribute("invoiceLines", dao.getDetails(order.getId()));
            } catch (IllegalArgumentException error) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } catch (SQLException error) {
                throw new ServletException("Không mở được hóa đơn", error);
            }
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!"/admin-invoice".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            int id = parseId(request);
            OrderDAO dao = new OrderDAO();
            Order order = dao.getOrderById(id);
            if (order == null || !order.isSuccessStatus()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String kind = field(request, "kind");
            String name = field(request, "buyer");
            String address = field(request, "address");
            String tax = field(request, "tax");
            boolean company = "company".equals(kind);
            if ((!company && !"personal".equals(kind)) || name.isEmpty() || name.length() > 160
                    || address.isEmpty() || address.length() > 255 || (company && (tax.isEmpty() || tax.length() > 30))) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Vui lòng kiểm tra thông tin người mua.");
                return;
            }
            List<OrderDetail> lines = dao.getDetails(id);
            if (lines.isEmpty()) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Không tìm thấy chi tiết mặt hàng để lập hóa đơn.");
                return;
            }
            InvoiceDAO.Buyer buyer = new InvoiceDAO.Buyer(kind, name, address, company ? tax : null);
            new InvoiceDAO().save(id, buyer);
            String outcome = "&saved=1";
            if ("email".equals(request.getParameter("action"))) {
                if (order.getContactEmail() == null || order.getContactEmail().isBlank()) {
                    outcome += "&mail=no-address";
                } else if (!EmailUtil.isConfigured()) {
                    outcome += "&mail=unavailable";
                } else {
                    try {
                        new InvoiceEmailService().send(order, buyer, lines);
                        outcome += "&mail=sent";
                    } catch (RuntimeException error) {
                        getServletContext().log("Không gửi được hóa đơn của đơn " + id, error);
                        outcome += "&mail=failed";
                    }
                }
            }
            response.sendRedirect(request.getContextPath() + "/admin-invoice?id=" + id + outcome);
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException error) {
            throw new ServletException("Không lưu được hóa đơn", error);
        }
    }

    private int parseId(HttpServletRequest request) {
        int id = Integer.parseInt(request.getParameter("id"));
        if (id <= 0) throw new IllegalArgumentException("Invalid order ID");
        return id;
    }

    private String field(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.strip();
    }
}
