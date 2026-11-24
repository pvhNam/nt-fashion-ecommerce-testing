package controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/validateOtp")
public class ValidateOtpController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    static final int MAX_ATTEMPTS = 5;
    static final long OTP_TTL_MILLIS = 5 * 60 * 1000L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        Integer otp = (Integer) session.getAttribute("otp");
        Long createdAt = (Long) session.getAttribute("otpCreatedAt");

        if (otp == null || createdAt == null || System.currentTimeMillis() - createdAt > OTP_TTL_MILLIS) {
            clearOtp(session);
            fail(request, response, "Mã OTP đã hết hạn hoặc không còn hiệu lực. Vui lòng yêu cầu mã mới.");
            return;
        }

        String input = request.getParameter("otp");
        if (input != null && input.trim().matches("\\d{6}") && Integer.parseInt(input.trim()) == otp) {
            clearOtp(session);
            session.setAttribute("otpVerified", Boolean.TRUE);
            request.setAttribute("email", session.getAttribute("email"));
            request.getRequestDispatcher("/new_password.jsp").forward(request, response);
            return;
        }

        Integer previous = (Integer) session.getAttribute("otpAttempts");
        int attempts = (previous == null ? 0 : previous) + 1;
        if (attempts >= MAX_ATTEMPTS) {
            clearOtp(session);
            fail(request, response, "Bạn đã nhập sai OTP " + MAX_ATTEMPTS + " lần. Vui lòng yêu cầu mã mới.");
            return;
        }
        session.setAttribute("otpAttempts", attempts);
        fail(request, response, "Mã OTP không chính xác!");
    }

    static void clearOtp(HttpSession session) {
        session.removeAttribute("otp");
        session.removeAttribute("otpCreatedAt");
        session.removeAttribute("otpAttempts");
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("message", message);
        request.getRequestDispatcher("/verify_otp.jsp").forward(request, response);
    }
}
