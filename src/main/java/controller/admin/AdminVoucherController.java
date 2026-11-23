package controller.admin;

import dao.VoucherDAO;
import model.Voucher;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
            List<Voucher> list = vDao.getAllVouchers();
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
        String type = request.getParameter("discountType");
        String amountInput = request.getParameter("discountAmount");
        String minOrderInput = request.getParameter("minOrder");
        double amount;
        double minOrder;
        try {
            amount = Double.parseDouble(amountInput == null ? "" : amountInput.trim());
            minOrder = Double.parseDouble(minOrderInput == null ? "" : minOrderInput.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect("admin-vouchers?msg=error_value_range");
            return;
        }

        if (!Double.isFinite(amount) || amount < 0
                || ("PERCENT".equals(type) && amount > 100)
                || !Double.isFinite(minOrder) || minOrder < 0) {
            response.sendRedirect("admin-vouchers?msg=error_value_range");
            return;
        }

        String expiryInput = request.getParameter("expiryDate");
        // Require the form's ISO date format within MySQL DATE's supported years.
        if (expiryInput == null || !expiryInput.matches("[1-9][0-9]{3}-[0-9]{2}-[0-9]{2}")) {
            response.sendRedirect("admin-vouchers?msg=error_expiry_date");
            return;
        }

        Date expiry;
        try {
            expiry = Date.valueOf(LocalDate.parse(expiryInput));
        } catch (DateTimeParseException e) {
            response.sendRedirect("admin-vouchers?msg=error_expiry_date");
            return;
        }

        if ("insert".equals(action)) {
            Voucher v = new Voucher(0, code, desc, amount, type, minOrder, expiry);
            vDao.insertVoucher(v);
        } else if ("update".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            Voucher v = new Voucher(id, code, desc, amount, type, minOrder, expiry);
            vDao.updateVoucher(v);
        }
        response.sendRedirect("admin-vouchers?msg=success");
    }
}
