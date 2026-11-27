<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Locale, java.text.NumberFormat, java.time.format.DateTimeFormatter, dao.InvoiceDAO.Buyer, model.Order, model.OrderDetail, util.Html" %>
<%
    Order order = (Order) request.getAttribute("invoiceOrder");
    List<Order> orders = (List<Order>) request.getAttribute("invoiceOrders");
    List<OrderDetail> lines = (List<OrderDetail>) request.getAttribute("invoiceLines");
    boolean admin = Boolean.TRUE.equals(request.getAttribute("invoiceAdmin"));
    Buyer buyer = (Buyer) request.getAttribute("invoiceBuyer");
    boolean company = buyer != null && buyer.isCompany();
    String buyerName = buyer == null && order != null ? order.getUserName() : buyer == null ? "" : buyer.name();
    String buyerAddress = buyer == null && order != null ? order.getAddress() : buyer == null ? "" : buyer.address();
    String buyerTax = buyer == null ? "" : buyer.taxCode();
    NumberFormat money = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    String base = request.getContextPath();
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hóa đơn đơn hàng | Nam Thành</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="<%=base%>/<%=admin ? "CSS/admin/Admin.css" : "CSS/style.css"%>">
    <link rel="stylesheet" href="<%=base%>/CSS/shared/customer-tools.css?v=20261126.1">
    <% if (admin) { %><script src="<%=base%>/JS/admin-invoice.js?v=20261126.1" defer></script><% } %>
</head>
<body class="invoice-page <%=admin ? "" : "workspace-public"%>">
<% if (admin) { %>
<jsp:include page="/sidebarAdmin.jsp"><jsp:param name="pageName" value="invoice" /></jsp:include>
<% } else { %><jsp:include page="/header.jsp" /><% } %>
<main class="<%=admin ? "main-content" : "workspace-main"%>">
    <div class="content-header workspace-controls">
        <h1 class="page-title">Hóa đơn đơn hàng</h1>
        <% if (admin) { %><a class="workspace-link" href="<%=base%>/admin-orders">Quản lý đơn hàng</a><% } else { %>
        <a class="workspace-link" href="<%=base%>/order-detail?id=<%=order.getId()%>">Chi tiết đơn hàng</a>
        <button type="button" class="workspace-button" onclick="window.print()">In hoặc lưu PDF</button>
        <% } %>
    </div>
    <% if (order == null) { %>
    <section class="workspace-card workspace-controls">
        <h2>Đơn đã giao thành công</h2>
        <% if (orders == null || orders.isEmpty()) { %><p>Chưa có đơn hàng phù hợp.</p><% } else { %>
        <form class="workspace-form" action="<%=base%>/admin-invoice" method="get">
            <label>Đơn hàng
                <select name="id" required>
                    <% for (Order item : orders) { %>
                    <option value="<%=item.getId()%>">#<%=item.getId()%> · <%=Html.escape(item.getUserName())%> · <%=money.format(item.getTotalMoney())%></option>
                    <% } %>
                </select>
            </label>
            <button class="workspace-button" type="submit">Lập hóa đơn</button>
        </form>
        <% } %>
    </section>
    <% } else { %>
    <% long subtotal = Math.round(lines.stream().mapToDouble(line -> line.getPrice() * line.getQuantity()).sum());
       long adjustment = Math.round(order.getTotalMoney()) - subtotal; %>
    <% if (admin && "1".equals(request.getParameter("saved"))) {
        String mail = request.getParameter("mail");
        String feedback = "Đã lưu hóa đơn. Khách có thể xem trong chi tiết đơn.";
        if ("sent".equals(mail)) feedback = "Đã lưu và gửi hóa đơn qua email.";
        else if ("no-address".equals(mail)) feedback = "Đã lưu hóa đơn. Đơn chưa có địa chỉ email để gửi.";
        else if ("unavailable".equals(mail)) feedback = "Đã lưu hóa đơn. Chưa cấu hình SMTP để gửi email.";
        else if ("failed".equals(mail)) feedback = "Đã lưu hóa đơn nhưng gửi email thất bại. Có thể gửi lại.";
    %><p class="workspace-notice workspace-controls" role="status"><%=feedback%></p><% } %>
    <div class="<%=admin ? "invoice-layout" : "invoice-customer-layout"%>">
        <% if (admin) { %>
        <form id="invoice-form" action="<%=base%>/admin-invoice" method="post" class="workspace-card workspace-form workspace-controls" data-personal-name="<%=Html.escape(order.getUserName())%>" data-personal-address="<%=Html.escape(order.getAddress())%>">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="id" value="<%=order.getId()%>">
            <h2>Thông tin người mua</h2>
            <label>Loại hóa đơn
                <select name="kind"><option value="personal">Cá nhân</option><option value="company" <%=company ? "selected" : ""%>>Công ty</option></select>
            </label>
            <label><span id="invoice-name-label"><%=company ? "Tên công ty" : "Họ và tên"%></span><input name="buyer" maxlength="160" required value="<%=Html.escape(buyerName)%>"></label>
            <label id="invoice-tax-field" <%=company ? "" : "hidden"%>>Mã số thuế công ty<input name="tax" maxlength="30" value="<%=Html.escape(buyerTax)%>" <%=company ? "required" : "disabled"%>></label>
            <label>Địa chỉ<textarea name="address" rows="3" maxlength="255" required><%=Html.escape(buyerAddress)%></textarea></label>
            <p class="workspace-muted">Email nhận hóa đơn · <%=order.getContactEmail() == null || order.getContactEmail().isBlank() ? "Chưa có" : Html.escape(order.getContactEmail())%></p>
            <button class="workspace-button" type="submit" name="action" value="save">Lưu cho khách xem</button>
            <button class="workspace-button" type="submit" name="action" value="email" <%=order.getContactEmail() == null || order.getContactEmail().isBlank() ? "disabled" : ""%>>Gửi hóa đơn qua email</button>
            <button class="workspace-button" type="submit" name="action" value="print" disabled>In hoặc lưu PDF</button>
            <a class="workspace-link" href="<%=base%>/admin-invoice">Chọn đơn khác</a>
        </form>
        <% } %>
        <article class="workspace-card invoice-paper" id="invoice-preview">
            <header class="invoice-heading">
                <div><strong>NAM THÀNH FASHION</strong><h2>Hóa đơn bán hàng</h2></div>
                <p>Đơn #<%=order.getId()%><br><span data-invoice="kind"><%=company ? "Công ty" : "Cá nhân"%></span>
                    <% if (order.getCreatedDateTime() != null) { %><br>Ngày đặt <%=order.getCreatedDateTime().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))%><% } %>
                </p>
            </header>
            <dl class="invoice-buyer">
                <div><dt>Người mua</dt><dd data-invoice="buyer"><%=Html.escape(buyerName)%></dd></div>
                <div data-invoice-tax <%=company ? "" : "hidden"%>><dt>Mã số thuế</dt><dd data-invoice="tax"><%=Html.escape(buyerTax)%></dd></div>
                <div><dt>Địa chỉ</dt><dd data-invoice="address"><%=Html.escape(buyerAddress)%></dd></div>
            </dl>
            <div class="invoice-table-wrap">
                <table class="invoice-table">
                    <thead><tr><th scope="col">Mặt hàng</th><th scope="col">SL</th><th scope="col">Đơn giá</th><th scope="col">Thành tiền</th></tr></thead>
                    <tbody>
                    <% if (lines.isEmpty()) { %><tr><td colspan="4">Chi tiết mặt hàng không còn trong hệ thống.</td></tr><% } %>
                    <% for (OrderDetail line : lines) { %>
                    <tr><td><%=Html.escape(line.getProduct().getPdescription())%><br><small><%=Html.escape(line.getColor())%> <%=Html.escape(line.getSize())%></small></td>
                        <td><%=line.getQuantity()%></td><td><%=money.format(line.getPrice())%></td><td><%=money.format(line.getPrice() * line.getQuantity())%></td></tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
            <% if (!lines.isEmpty() && adjustment != 0) { %>
            <p class="invoice-total">Tạm tính <span><%=money.format(subtotal)%></span></p>
            <p class="invoice-total">Điều chỉnh đơn hàng <span><%=money.format(adjustment)%></span></p>
            <% } %>
            <p class="invoice-total">Tổng tiền đơn hàng <strong><%=money.format(order.getTotalMoney())%></strong></p>
            <p class="workspace-muted">Bản in từ đơn hàng, dùng để đối chiếu thông tin mua hàng.</p>
        </article>
    </div>
    <% } %>
</main>
<% if (!admin) { %><jsp:include page="/footer.jsp" /><% } %>
</body>
</html>
