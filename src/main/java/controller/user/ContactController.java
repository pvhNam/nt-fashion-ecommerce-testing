package controller.user;

import dao.ContactDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.User;
import util.AdminAccess;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet({"/contact", "/admin-contact"})
public class ContactController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean admin = "/admin-contact".equals(request.getServletPath());
        User user = AdminAccess.currentUser(request);
        response.setHeader("Cache-Control", "no-store");
        request.setAttribute("adminContact", admin);
        request.setAttribute("contactUser", user);
        if (user != null) {
            try {
                int id = threadId(request);
                ContactDAO dao = new ContactDAO();
                List<ContactDAO.Thread> threads = dao.list(user.getUid(), admin);
                request.setAttribute("threads", threads);
                if (id > 0) {
                    ContactDAO.Thread thread = threads.stream().filter(item -> item.id() == id).findFirst().orElse(null);
                    if (thread == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    request.setAttribute("thread", thread);
                    request.setAttribute("messages", dao.messages(id, user.getUid(), admin));
                }
            } catch (IllegalArgumentException error) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } catch (SQLException error) {
                throw new ServletException("Không tải được hộp thư liên hệ", error);
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/contact.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = AdminAccess.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        boolean admin = "/admin-contact".equals(request.getServletPath());
        try {
            int id = threadId(request);
            String subject = value(request, "subject");
            String body = value(request, "body");
            if (body.isEmpty() || body.length() > 2000
                    || (id == 0 && (admin || subject.isEmpty() || subject.length() > 160))) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Vui lòng kiểm tra lại chủ đề và nội dung.");
                return;
            }
            int result = new ContactDAO().send(id, user.getUid(), admin, subject, body);
            if (result == 0) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.sendRedirect(request.getContextPath() + request.getServletPath() + "?id=" + result + "&sent=1");
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException error) {
            throw new ServletException("Không gửi được tin nhắn", error);
        }
    }

    private int threadId(HttpServletRequest request) {
        String raw = request.getParameter("id");
        if (raw == null) return 0;
        int id = Integer.parseInt(raw);
        if (id <= 0) throw new IllegalArgumentException("Invalid contact thread ID");
        return id;
    }

    private String value(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? "" : value.strip();
    }
}
