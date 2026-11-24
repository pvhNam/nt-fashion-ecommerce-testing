package controller.admin;

import dao.VoucherDAO;
import model.Voucher;
import java.io.IOException;
import java.sql.Date;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin-vouchers")
public class AdminVoucherController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        VoucherDAO vDao = new VoucherDAO();

        if (action == null) {
            List<Voucher> list = vDao.getAllVouchersForAdmin();
            request.setAttribute("listVouchers", list);
            request.getRequestDispatcher("/admin-vouchers.jsp").forward(request, response);
        } else if (action.equals("edit")) {
            int id = Integer.parseInt(request.getParameter("id"));
            Voucher v = vDao.getVoucherById(id);
            request.setAttribute("voucher", v);
            request.getRequestDispatcher("/admin-voucher-form.jsp").forward(request, response);
        } else if (action.equals("add")) {
            request.getRequestDispatcher("/admin-voucher-form.jsp").forward(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        VoucherDAO vDao = new VoucherDAO();

        if ("delete".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            vDao.deleteVoucher(id);
            response.sendRedirect("admin-vouchers?msg=deleted");
            return;
        }

        String code = request.getParameter("code");
        String desc = request.getParameter("description");
        double amount = Double.parseDouble(request.getParameter("discountAmount"));
        String type = request.getParameter("discountType");
        double minOrder = Double.parseDouble(request.getParameter("minOrder"));

        if (!Double.isFinite(amount) || amount < 0
                || ("PERCENT".equals(type) && amount > 100)
                || !Double.isFinite(minOrder) || minOrder < 0) {
            response.sendRedirect("admin-vouchers?msg=error_value_range");
            return;
        }

        Date expiry = Date.valueOf(request.getParameter("expiryDate"));
        String startParam = request.getParameter("startDate");
        Date start = (startParam == null || startParam.isBlank()) ? null : Date.valueOf(startParam);
        if (start != null && start.after(expiry)) {
            response.sendRedirect("admin-vouchers?msg=error_date_range");
            return;
        }

        int id = "update".equals(action) ? Integer.parseInt(request.getParameter("id")) : 0;
        Voucher v = new Voucher(id, code, desc, amount, type, minOrder, expiry);
        v.setStartDate(start);
        v.setPublicVoucher(request.getParameter("isPublic") != null);
        if ("insert".equals(action)) {
            vDao.insertVoucher(v);
        } else if ("update".equals(action)) {
            vDao.updateVoucher(v);
        }
        response.sendRedirect("admin-vouchers?msg=success");
    }
}
