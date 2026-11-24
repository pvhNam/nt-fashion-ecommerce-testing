package controller.user;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dao.CartDAO;
import dao.LoyaltyDAO;
import dao.ProductDAO;
import dao.VoucherDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.CartItem;
import model.Membership;
import model.Product;
import model.User;
import model.Voucher;
import util.I18n;
import org.json.JSONObject;
import service.PendingPaymentService;
import service.PricingService;

@WebServlet("/cart")
public class CartController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        List<Voucher> myVouchers = new ArrayList<>();

        if (currentUser != null) {
            // Hủy đơn online quá hạn chờ thanh toán để voucher đã giữ quay lại ví (IS-05)
            new PendingPaymentService().expireOverdue(currentUser.getUid());
            VoucherDAO vDao = new VoucherDAO();
            myVouchers = vDao.getVouchersByUid(currentUser.getUid());
        }

        List<CartItem> cart = getCurrentCart(session, currentUser);
        Map<CartItem, Integer> stock = getCartStock(cart, currentUser);
        double subtotal = calculateSubtotal(cart);

        Membership membership = currentUser == null
                ? Membership.standard(0)
                : new LoyaltyDAO().getMembership(currentUser.getUid());
        Voucher appliedVoucher = (Voucher) session.getAttribute("appliedVoucher");
        if (appliedVoucher != null) {
            if (cart.isEmpty() || currentUser == null) {
                session.removeAttribute("appliedVoucher");
                appliedVoucher = null;
            } else {
                Voucher refreshedVoucher = new VoucherDAO().getAvailableVoucherForUserById(
                        currentUser.getUid(), appliedVoucher.getId());
                if (refreshedVoucher == null) {
                    session.removeAttribute("appliedVoucher");
                    session.setAttribute("voucherMsg", "Voucher da het han, da su dung hoac khong con trong vi.");
                    session.setAttribute("msgType", "error");
                    appliedVoucher = null;
                } else if (subtotal < refreshedVoucher.getMinOrder()) {
                    session.removeAttribute("appliedVoucher");
                    session.setAttribute("voucherMsg", "Ma " + refreshedVoucher.getCode()
                            + " da bi huy do don hang chua du " + (long) refreshedVoucher.getMinOrder() + "d");
                    session.setAttribute("msgType", "error");
                    appliedVoucher = null;
                } else {
                    appliedVoucher = refreshedVoucher;
                    session.setAttribute("appliedVoucher", refreshedVoucher);
                }
            }
        }
        PricingService.Quote quote = PricingService.calculate(
                subtotal, appliedVoucher, membership.getDiscountPercent());

        request.setAttribute("cartList", cart);
        request.setAttribute("cartStock", stock);
        request.setAttribute("cartHasError", exceedsStock(cart, stock));
        request.setAttribute("subtotal", subtotal);
        request.setAttribute("discountAmount", quote.getTotalDiscount());
        request.setAttribute("voucherDiscountAmount", quote.getVoucherDiscount());
        request.setAttribute("memberDiscountAmount", quote.getMemberDiscount());
        request.setAttribute("memberDiscountPercent", quote.getMemberDiscountPercent());
        request.setAttribute("memberTier", membership.getTierLabel());
        request.setAttribute("memberLifetimeSpend", membership.getLifetimeSpend());
        request.setAttribute("finalTotal", quote.getFinalTotal());
        request.setAttribute("totalCount", cart.size());
        request.setAttribute("myVouchers", myVouchers);
        request.setAttribute("walletCount", myVouchers.size());
        session.setAttribute("cartCount", cart.size());

        request.getRequestDispatcher("/cartitem.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        String action = request.getParameter("action");
        boolean ajax = isAjax(request);

        if ("apply_voucher".equals(action)) {
            if (currentUser == null) {
                if (ajax) {
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Vui lòng đăng nhập để sử dụng voucher.", true);
                } else {
                    response.sendRedirect("login");
                }
                return;
            }

            List<CartItem> cart = getCurrentCart(session, currentUser);
            if (cart.isEmpty()) {
                session.removeAttribute("appliedVoucher");
                String message = I18n.t(request, "cart.empty");
                if (ajax) {
                    writeCartState(request, response, currentUser, message, false);
                } else {
                    session.setAttribute("voucherMsg", message);
                    session.setAttribute("msgType", "error");
                    response.sendRedirect("cart");
                }
                return;
            }
            String code = request.getParameter("voucherCode");
            if (code != null) code = code.trim().toUpperCase();

            new PendingPaymentService().expireOverdue(currentUser.getUid());
            VoucherDAO vDao = new VoucherDAO();
            Voucher knownVoucher = vDao.getVoucherByCode(code);
            Voucher v = vDao.getAvailableVoucherForUserByCode(currentUser.getUid(), code);

            // Public codes can be typed directly at checkout. Claim the code into
            // the user's wallet automatically, while private reward codes still
            // require ownership in the wallet.
            if (v == null) {
                Voucher publicVoucher = vDao.getPublicVoucherByCode(code);
                if (publicVoucher != null
                        && vDao.saveVoucherToWallet(currentUser.getUid(), publicVoucher.getId())) {
                    v = vDao.getAvailableVoucherForUserById(currentUser.getUid(), publicVoucher.getId());
                }
            }

            if (knownVoucher == null) {
                session.setAttribute("voucherMsg", I18n.t(request, "voucher.msg.not_found"));
                session.setAttribute("msgType", "error");
                session.removeAttribute("appliedVoucher");
            } else if (v == null && !knownVoucher.isStarted()) {
                session.setAttribute("voucherMsg", I18n.t(request, "voucher.msg.not_started",
                        knownVoucher.getCode(),
                        knownVoucher.getStartDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                session.setAttribute("msgType", "error");
                session.removeAttribute("appliedVoucher");
            } else if (v == null) {
                session.setAttribute("voucherMsg", I18n.t(request, "voucher.msg.unavailable"));
                session.setAttribute("msgType", "error");
                session.removeAttribute("appliedVoucher");
            } else if (calculateSubtotal(cart) < v.getMinOrder()) {
                session.setAttribute("voucherMsg",
                        I18n.t(request, "voucher.msg.min_order", (long) v.getMinOrder(), v.getCode()));
                session.setAttribute("msgType", "error");
                session.removeAttribute("appliedVoucher");
            } else {
                session.setAttribute("appliedVoucher", v);
                session.setAttribute("voucherMsg", I18n.t(request, "voucher.msg.applied", v.getCode()));
                session.setAttribute("msgType", "success");
            }
            if (ajax) {
                String message = (String) session.getAttribute("voucherMsg");
                boolean success = "success".equals(session.getAttribute("msgType"));
                writeCartState(request, response, currentUser, message, success);
                session.removeAttribute("voucherMsg");
                session.removeAttribute("msgType");
                return;
            }
            response.sendRedirect("cart");
            return;
        }

        if ("remove_voucher".equals(action)) {
            session.removeAttribute("appliedVoucher");
            session.setAttribute("voucherMsg", "Da go bo ma giam gia.");
            session.setAttribute("msgType", "info");
            if (ajax) {
                writeCartState(request, response, currentUser, "Đã gỡ voucher.", true);
                session.removeAttribute("voucherMsg");
                session.removeAttribute("msgType");
                return;
            }
            response.sendRedirect("cart");
            return;
        }
 if (currentUser == null && ("add".equals(action) || "buyNow".equals(action))) {
            if (ajax) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.", true);
            } else {
                response.sendRedirect("login");
            }
            return;
        }
        int requestedQuantity = parseInt(request.getParameter("quantity"),
                "update_quantity".equals(action) ? 0 : 1);
        if (requestedQuantity <= 0 && ("add".equals(action) || "buyNow".equals(action)
                || "update_quantity".equals(action))) {
            if (ajax) {
                writeCartState(request, response, currentUser,
                        "Số lượng phải là số nguyên dương hợp lệ.", false);
            } else {
                session.setAttribute("toastMessage", "Số lượng phải là số nguyên dương hợp lệ.");
                session.setAttribute("toastType", "error");
                response.sendRedirect("cart");
            }
            return;
        }

        int pid = 0;
        try { if (request.getParameter("pid") != null) pid = Integer.parseInt(request.getParameter("pid")); } catch (Exception e) {}

        String color = request.getParameter("color");
        String size = request.getParameter("size");
        color = color == null ? "" : color.trim();
        size = size == null ? "" : size.trim();

        ProductDAO productDAO = new ProductDAO();

        if ("remove".equals(action)) {
            removeCartItem(session, currentUser, pid, color, size);
            if (ajax) {
                writeCartState(request, response, currentUser,
                        "Đã xóa sản phẩm khỏi giỏ hàng.", true);
            } else {
                clearVoucherIfCartEmpty(session, currentUser);
                response.sendRedirect("cart");
            }
            return;
        }

        if ("update_quantity".equals(action)) {
            String mod = request.getParameter("mod");
            long newQty = requestedQuantity + ("increase".equals(mod) ? 1L : -1L);
            int availableStock = productDAO.getAvailableStock(pid, color, size);
            String error = null;
            boolean updated = false;
            if (!"increase".equals(mod) && !"decrease".equals(mod)) {
                error = "Thao tác cập nhật số lượng không hợp lệ.";
            } else if ("increase".equals(mod) && newQty > availableStock) {
                error = "Sản phẩm đã đạt giới hạn tồn kho.";
            } else if (currentUser != null) {
                updated = new CartDAO().updateQuantityIfCurrent(currentUser.getUid(), pid, color, size,
                        requestedQuantity, (int) newQty);
            } else {
                synchronized (session) {
                    List<CartItem> cart = getGuestCart(session);
                    Iterator<CartItem> iterator = cart.iterator();
                    while (iterator.hasNext()) {
                        CartItem item = iterator.next();
                        String itemColor = item.getColor() == null ? "" : item.getColor();
                        String itemSize = item.getSize() == null ? "" : item.getSize();
                        if (item.getProduct().getPid() == pid && itemColor.equals(color) && itemSize.equals(size)) {
                            item.getProduct().setStockquantyti(availableStock);
                            if (item.getQuantity() == requestedQuantity) {
                                if (newQty == 0) iterator.remove();
                                else item.setQuantity((int) newQty);
                                session.setAttribute("cart", cart);
                                updated = true;
                            }
                            break;
                        }
                    }
                }
            }
            if (error == null && !updated) {
                error = "Giỏ hàng đã thay đổi. Vui lòng kiểm tra số lượng mới và thử lại.";
            }
            if (ajax) {
                writeCartState(request, response, currentUser, error, error == null);
            } else {
                if (updated && newQty == 0) clearVoucherIfCartEmpty(session, currentUser);
                if (error != null) {
                    session.setAttribute("toastMessage", error);
                    session.setAttribute("toastType", "error");
                }
                response.sendRedirect("cart");
            }
            return;
        }

        if (currentUser != null) {
            CartDAO dao = new CartDAO();
            int uid = currentUser.getUid();

            if ("add".equals(action) || "buyNow".equals(action)) {
                int quantity = requestedQuantity;
                int currentQuantity = 0;
                for (CartItem item : dao.getCartByUid(uid)) {
                    String itemColor = item.getColor() == null ? "" : item.getColor();
                    String itemSize = item.getSize() == null ? "" : item.getSize();
                    if (item.getProduct().getPid() == pid && itemColor.equals(color) && itemSize.equals(size)) {
                        currentQuantity = item.getQuantity();
                        break;
                    }
                }
                int availableStock = productDAO.getAvailableStock(pid, color, size);
                if (quantity <= 0 || availableStock <= 0 || (long) currentQuantity + quantity > availableStock) {
                    if (ajax) {
                        writeCartState(request, response, currentUser,
                                "Phân loại đã hết hàng hoặc số lượng vượt quá tồn kho.", false);
                    } else {
                        session.setAttribute("toastMessage",
                                "Phân loại đã hết hàng hoặc số lượng vượt quá tồn kho.");
                        session.setAttribute("toastType", "error");
                        response.sendRedirect("cart");
                    }
                    return;
                }

                // Lưu vào DB kèm theo Màu và Size
                dao.addToCart(uid, pid, color, size, quantity);

                if ("buyNow".equals(action)) response.sendRedirect("checkout");
                else response.sendRedirect("cart"); // Thêm xong chuyển ra giỏ hàng
                return;

            }
        } else {
            // khách chưa log(Lưu session)
            List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
            if (cart == null) cart = new ArrayList<>();

            if ("add".equals(action) || "buyNow".equals(action)) {
                int quantity = requestedQuantity;
                Product p = productDAO.getProductById(pid);
                boolean exists = false;
                int currentQuantity = 0;
                for (CartItem item : cart) {
                    if (item.getProduct().getPid() == pid && item.getColor().equals(color) && item.getSize().equals(size)) {
                        currentQuantity = item.getQuantity();
                        exists = true;
                        break;
                    }
                }
                int maxStock = productDAO.getAvailableStock(pid, color, size);
                if (p == null || quantity <= 0 || maxStock <= 0 || (long) currentQuantity + quantity > maxStock) {
                    if (ajax) {
                        writeCartState(request, response, null,
                                "Phân loại đã hết hàng hoặc số lượng vượt quá tồn kho.", false);
                    } else {
                        session.setAttribute("toastMessage",
                                "Phân loại đã hết hàng hoặc số lượng vượt quá tồn kho.");
                        session.setAttribute("toastType", "error");
                        response.sendRedirect("cart");
                    }
                    return;
                }
                p.setStockquantyti(maxStock);
                if (exists) {
                    for (CartItem item : cart) {
                        if (item.getProduct().getPid() == pid && item.getColor().equals(color) && item.getSize().equals(size)) {
                            item.setQuantity(currentQuantity + quantity);
                            item.getProduct().setStockquantyti(maxStock);
                            break;
                        }
                    }
                } else {
                    cart.add(new CartItem(p, quantity, color, size));
                }
                session.setAttribute("cart", cart);
                if ("buyNow".equals(action)) { response.sendRedirect("checkout"); return; }
            }
        }
        if (ajax) {
            writeCartState(request, response, currentUser, null, true);
            return;
        }
        response.sendRedirect("cart");
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }

    private void removeCartItem(HttpSession session, User currentUser, int pid, String color, String size) {
        if (currentUser != null) {
            new CartDAO().removeItem(currentUser.getUid(), pid, color, size);
            return;
        }

        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) return;
        Iterator<CartItem> iterator = cart.iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            String itemColor = item.getColor() == null ? "" : item.getColor();
            String itemSize = item.getSize() == null ? "" : item.getSize();
            if (item.getProduct().getPid() == pid && itemColor.equals(color) && itemSize.equals(size)) {
                iterator.remove();
                break;
            }
        }
        session.setAttribute("cart", cart);
    }

    private List<CartItem> getGuestCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        return cart != null ? cart : new ArrayList<>();
    }

    private List<CartItem> getCurrentCart(HttpSession session, User currentUser) {
        if (currentUser != null) return new CartDAO().getCartByUid(currentUser.getUid());
        return new ArrayList<>(getGuestCart(session));
    }

    private Map<CartItem, Integer> getCartStock(List<CartItem> cart, User user) {
        if (user == null) return new ProductDAO().getGuestCartStock(cart);
        Map<CartItem, Integer> stock = new HashMap<>();
        for (CartItem item : cart) {
            stock.put(item, item.getProduct().getStockquantyti());
        }
        return stock;
    }

    private boolean exceedsStock(List<CartItem> cart, Map<CartItem, Integer> stock) {
        return cart.stream().anyMatch(item -> item.getQuantity() > stock.getOrDefault(item, 0));
    }

    private void clearVoucherIfCartEmpty(HttpSession session, User user) {
        if (session.getAttribute("appliedVoucher") != null && getCurrentCart(session, user).isEmpty()) {
            session.removeAttribute("appliedVoucher");
        }
    }

    private void writeCartState(HttpServletRequest request, HttpServletResponse response,
                                User currentUser, String message, boolean success) throws IOException {
        HttpSession session = request.getSession();
        List<CartItem> cart = getCurrentCart(session, currentUser);
        Map<CartItem, Integer> stock = getCartStock(cart, currentUser);
        double subtotal = calculateSubtotal(cart);

        Membership membership = currentUser == null
                ? Membership.standard(0)
                : new LoyaltyDAO().getMembership(currentUser.getUid());
        boolean applyingVoucher = "apply_voucher".equals(request.getParameter("action"));
        Voucher appliedVoucher = (Voucher) session.getAttribute("appliedVoucher");
        if (appliedVoucher != null && (cart.isEmpty() || currentUser == null)) {
            session.removeAttribute("appliedVoucher");
            appliedVoucher = null;
            if (applyingVoucher && success) {
                message = I18n.t(request, "cart.empty");
                success = false;
            }
        }
        if (appliedVoucher != null) {
            Voucher refreshedVoucher = new VoucherDAO().getAvailableVoucherForUserById(
                    currentUser.getUid(), appliedVoucher.getId());
            if (refreshedVoucher == null || subtotal < refreshedVoucher.getMinOrder()) {
                session.removeAttribute("appliedVoucher");
                if (success) {
                    message = refreshedVoucher == null ? I18n.t(request, "voucher.msg.unavailable")
                            : I18n.t(request, "voucher.msg.min_order", (long) refreshedVoucher.getMinOrder(),
                                    refreshedVoucher.getCode());
                    success = !applyingVoucher;
                }
                appliedVoucher = null;
            } else {
                appliedVoucher = refreshedVoucher;
                session.setAttribute("appliedVoucher", refreshedVoucher);
            }
        }
        PricingService.Quote quote = PricingService.calculate(
                subtotal, appliedVoucher, membership.getDiscountPercent());

        int pid = parseInt(request.getParameter("pid"), 0);
        String color = request.getParameter("color") == null ? "" : request.getParameter("color");
        String size = request.getParameter("size") == null ? "" : request.getParameter("size");
        CartItem changedItem = null;
        for (CartItem item : cart) {
            String itemColor = item.getColor() == null ? "" : item.getColor();
            String itemSize = item.getSize() == null ? "" : item.getSize();
            if (item.getProduct().getPid() == pid && itemColor.equals(color) && itemSize.equals(size)) {
                changedItem = item;
                break;
            }
        }

        session.setAttribute("cartCount", cart.size());
        JSONObject json = new JSONObject();
        json.put("success", success);
        json.put("message", message == null ? JSONObject.NULL : message);
        json.put("cartCount", cart.size());
        json.put("subtotal", subtotal);
        json.put("discountAmount", quote.getTotalDiscount());
        json.put("voucherDiscountAmount", quote.getVoucherDiscount());
        json.put("memberDiscountAmount", quote.getMemberDiscount());
        json.put("memberDiscountPercent", quote.getMemberDiscountPercent());
        json.put("finalTotal", quote.getFinalTotal());
        json.put("cartHasError", exceedsStock(cart, stock));
        json.put("removed", pid > 0 && changedItem == null);
        json.put("itemQuantity", changedItem != null ? changedItem.getQuantity() : 0);
        json.put("itemTotal", changedItem != null ? changedItem.getTotalPrice() : 0);
        json.put("itemStock", stock.getOrDefault(changedItem, 0));

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }

    private int parseInt(String value, int missingValue) {
        if (value == null) return missingValue;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return 0; }
    }

    private double calculateSubtotal(List<CartItem> cart) {
        double subtotal = 0;
        if (cart != null) {
            for (CartItem item : cart) subtotal += item.getTotalPrice();
        }
        return subtotal;
    }

    private void writeError(HttpServletResponse response, int status, String message,
                            boolean loginRequired) throws IOException {
        JSONObject json = new JSONObject();
        json.put("success", false);
        json.put("message", message);
        json.put("loginRequired", loginRequired);
        json.put("redirect", "login");
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }
}
