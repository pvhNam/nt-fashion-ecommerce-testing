package controller.admin;

import dao.CampaignDAO;
import dao.CampaignDAO.Campaign;
import dao.VoucherDAO;
import model.User;
import util.AdminAccess;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet({"/admin-campaigns", "/campaigns"})
public class CampaignController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean admin = "/admin-campaigns".equals(request.getServletPath());
        try {
            List<Campaign> campaigns = new CampaignDAO().list(!admin);
            request.setAttribute("adminCampaigns", admin);
            request.setAttribute("campaigns", campaigns);
            if (admin) {
                response.setHeader("Cache-Control", "no-store");
                request.setAttribute("campaignVouchers", new VoucherDAO().getAllVouchersForAdmin().stream()
                        .filter(model.Voucher::isPublicVoucher).toList());
                int id = optionalId(request, "id");
                if (id > 0) {
                    Campaign selected = campaigns.stream().filter(item -> item.id() == id).findFirst().orElse(null);
                    if (selected == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    request.setAttribute("selectedCampaign", selected);
                }
            }
            request.getRequestDispatcher("/WEB-INF/views/campaigns.jsp").forward(request, response);
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException error) {
            throw new ServletException("Không tải được chiến dịch", error);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!"/admin-campaigns".equals(request.getServletPath())) {
            User user = AdminAccess.currentUser(request);
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            try {
                int id = optionalId(request, "campaignId");
                String outcome = new CampaignDAO().claimVoucher(id, user.getUid());
                response.sendRedirect(request.getContextPath() + "/campaigns?claim=" + outcome);
            } catch (IllegalArgumentException error) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } catch (SQLException error) {
                throw new ServletException("Không lưu được voucher chiến dịch", error);
            }
            return;
        }
        try {
            int id = optionalId(request, "id");
            int voucher = optionalId(request, "voucherId");
            String title = value(request, "title");
            String description = value(request, "description");
            LocalDate start = LocalDate.parse(value(request, "start"));
            LocalDate end = LocalDate.parse(value(request, "end"));
            if (title.isEmpty() || title.length() > 160 || description.isEmpty() || description.length() > 2000
                    || start.getYear() < 1000 || end.getYear() > 9999 || end.isBefore(start)
                    || (voucher > 0 && new VoucherDAO().getAllVouchersForAdmin().stream()
                            .noneMatch(item -> item.isPublicVoucher() && item.getId() == voucher))) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Vui lòng kiểm tra nội dung, ngày và voucher của chiến dịch.");
                return;
            }
            boolean saved = new CampaignDAO().save(new Campaign(id, title, description, start, end,
                    voucher == 0 ? null : voucher, null, "on".equals(request.getParameter("published"))));
            if (!saved) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin-campaigns?saved=1");
        } catch (IllegalArgumentException | DateTimeParseException error) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Vui lòng kiểm tra ngày và mã chiến dịch.");
        } catch (SQLException error) {
            throw new ServletException("Không lưu được chiến dịch", error);
        }
    }

    private int optionalId(HttpServletRequest request, String field) {
        String raw = value(request, field);
        if (raw.isEmpty()) return 0;
        int id = Integer.parseInt(raw);
        if (id <= 0) throw new IllegalArgumentException("Invalid ID");
        return id;
    }

    private String value(HttpServletRequest request, String field) {
        String raw = request.getParameter(field);
        return raw == null ? "" : raw.strip();
    }
}
