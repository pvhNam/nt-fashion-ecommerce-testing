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

@WebServlet(name = "EditAddressServlet", urlPatterns = {"/edit-address"})
public class EditAddressServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int addressId = parseAddressId(request.getParameter("id"));
        if (addressId <= 0) {
            response.sendRedirect("address-list");
            return;
        }

        AddressDAO addressDAO = new AddressDAO();
        Address address = addressDAO.getAddressByIdAndUserId(addressId, user.getUid());
        if (address == null) {
            response.sendRedirect("address-list");
            return;
        }

        request.setAttribute("editAddress", address);
        request.getRequestDispatcher("add-address.jsp").forward(request, response);
    }

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

        int addressId = parseAddressId(request.getParameter("id"));
        if (addressId <= 0) {
            response.sendRedirect("address-list");
            return;
        }

        String receiverName = InputValidator.trim(request.getParameter("receiverName"));
        String rawPhone = InputValidator.trim(request.getParameter("phone"));
        String phone = InputValidator.normalizePhone(rawPhone);
        String city = InputValidator.trim(request.getParameter("city"));
        String district = InputValidator.trim(request.getParameter("district"));
        String ward = InputValidator.trim(request.getParameter("ward"));
        String specificAddress = InputValidator.trim(request.getParameter("specificAddress"));
        boolean isDefault = request.getParameter("isDefault") != null;

        // IS-13: cùng quy tắc với thêm địa chỉ; sai thì quay lại form sửa, không lưu
        String error = InputValidator.addressError(receiverName, phone, specificAddress, city, district, ward);
        if (error != null) {
            Address current = new AddressDAO().getAddressByIdAndUserId(addressId, user.getUid());
            if (current == null) {
                response.sendRedirect("address-list");
                return;
            }
            request.setAttribute("editAddress", current);
            request.setAttribute("addressFormError", error);
            request.setAttribute("formAddress", new Address(addressId, user.getUid(), receiverName, rawPhone,
                    specificAddress, ward, district, city, isDefault));
            request.getRequestDispatcher("add-address.jsp").forward(request, response);
            return;
        }

        Address address = new Address(
                addressId,
                user.getUid(),
                receiverName,
                phone,
                specificAddress,
                ward,
                district,
                city,
                isDefault
        );

        AddressDAO addressDAO = new AddressDAO();
        addressDAO.updateAddress(address);

        response.sendRedirect("address-list");
    }

    private int parseAddressId(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
