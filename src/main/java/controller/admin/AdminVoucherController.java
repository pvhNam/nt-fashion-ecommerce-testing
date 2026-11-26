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
    // Require ISO dates within MySQL DATE's supported years.
    private static final String DATE_FORMAT = "[1-9][0-9]{3}-[0-9]{2}-[0-9]{2}";

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
        if (!"insert".equals(action) && !"update".equals(action) && !"delete".equals(action)) {
            response.sendRedirect("admin-vouchers?msg=error_action");
            return;
        }
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
        if (code == null || code.isBlank()) {
            response.sendRedirect("admin-vouchers?msg=error_code_required");
            return;
        }
        if (!"FIXED".equals(type) && !"PERCENT".equals(type)) {
            response.sendRedirect("admin-vouchers?msg=error_discount_type");
            return;
        }
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
        if (expiryInput == null || !expiryInput.matches(DATE_FORMAT)) {
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

        String startInput = request.getParameter("startDate");
        Date start = null;
        if (startInput != null && !startInput.isBlank()) {
            if (!startInput.matches(DATE_FORMAT)) {
                response.sendRedirect("admin-vouchers?msg=error_start_date");
                return;
            }
            try {
                start = Date.valueOf(LocalDate.parse(startInput));
            } catch (DateTimeParseException e) {
                response.sendRedirect("admin-vouchers?msg=error_start_date");
                return;
            }
            if (start.after(expiry)) {
                response.sendRedirect("admin-vouchers?msg=error_date_range");
                return;
            }
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
