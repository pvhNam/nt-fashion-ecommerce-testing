package controller.user;

import dao.AddressDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Address;
import model.User;
import util.InputValidator;

@WebServlet(name = "AddAddressServlet", urlPatterns = {"/add-address"})
public class AddAddressServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        request.getRequestDispatcher("add-address.jsp").forward(request, response);
    }

    // Xử lý dữ liệu form gửi lên
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String receiverName = InputValidator.trim(request.getParameter("receiverName"));
        String rawPhone = InputValidator.trim(request.getParameter("phone"));
        String phone = InputValidator.normalizePhone(rawPhone);
        String city = InputValidator.trim(request.getParameter("city"));
        String district = InputValidator.trim(request.getParameter("district"));
        String ward = InputValidator.trim(request.getParameter("ward"));
        String specificAddress = InputValidator.trim(request.getParameter("specificAddress"));
        boolean isDefault = request.getParameter("isDefault") != null; // checkbox

        // IS-13: bắt buộc tên người nhận, SĐT hợp lệ, địa chỉ cụ thể; sai thì quay lại form, không lưu
        String error = InputValidator.addressError(receiverName, phone, specificAddress, city, district, ward);
        if (error != null) {
            request.setAttribute("addressFormError", error);
            request.setAttribute("formAddress", new Address(0, user.getUid(), receiverName, rawPhone,
                    specificAddress, ward, district, city, isDefault));
            request.getRequestDispatcher("add-address.jsp").forward(request, response);
            return;
        }

        Address newAddr = new Address(0, user.getUid(), receiverName, phone, specificAddress, ward, district, city, isDefault);

        AddressDAO addressDAO = new AddressDAO();
        addressDAO.insertAddress(newAddr);

        response.sendRedirect("address-list");
    }
}