package controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import dao.UserDAO;
import model.User;
import util.EmailUtil;
import java.io.IOException;
import java.util.Random;

@WebServlet("/forgotPassword")
public class ForgotPasswordController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SENT_MESSAGE = "Neu email co trong he thong, ma OTP da duoc gui den email do.";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/forgot_password.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        UserDAO dao = new UserDAO();
        User u = dao.checkEmailExist(email);

        if (u != null) {
            Random rand = new Random();
            int otpValue = 100000 + rand.nextInt(900000);

            String subject = "Ma xac thuc dat lai mat khau";
            String message = "Ma OTP cua ban la: " + otpValue;

            try {
                EmailUtil.sendEmail(email, subject, message);
            } catch (Exception e) {
                request.setAttribute("message", "Loi gui mail! Vui long thu lai.");
                request.getRequestDispatcher("/forgot_password.jsp").forward(request, response);
                return;
            }

            HttpSession session = request.getSession();
            ValidateOtpController.clearOtp(session);
            session.removeAttribute("otpVerified");
            session.setAttribute("otp", otpValue);
            session.setAttribute("otpCreatedAt", System.currentTimeMillis());
            session.setAttribute("email", email);
            session.setAttribute("resetUsername", u.getUsername());

            request.setAttribute("message", SENT_MESSAGE);
            request.getRequestDispatcher("/verify_otp.jsp").forward(request, response);

        } else {
            // Trả lời giống email có thật để không lộ email nào đã đăng ký
            HttpSession session = request.getSession();
            ValidateOtpController.clearOtp(session);
            session.removeAttribute("otpVerified");
            session.removeAttribute("email");
            session.removeAttribute("resetUsername");
            request.setAttribute("message", SENT_MESSAGE);
            request.getRequestDispatcher("/verify_otp.jsp").forward(request, response);
        }
    }
}
