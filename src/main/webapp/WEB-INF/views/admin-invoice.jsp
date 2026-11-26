<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.Locale, java.text.NumberFormat, java.time.format.DateTimeFormatter, model.Order, model.OrderDetail, util.Html" %>
<%
    Order order = (Order) request.getAttribute("invoiceOrder");
    List<Order> orders = (List<Order>) request.getAttribute("invoiceOrders");
    List<OrderDetail> lines = (List<OrderDetail>) request.getAttribute("invoiceLines");
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
    <link rel="stylesheet" href="<%=base%>/CSS/admin/Admin.css">
    <link rel="stylesheet" href="<%=base%>/CSS/shared/customer-tools.css?v=20261126">
    <script src="<%=base%>/JS/admin-invoice.js" defer></script>
</head>
<body class="invoice-page">
<jsp:include page="/sidebarAdmin.jsp"><jsp:param name="pageName" value="invoice" /></jsp:include>
<main class="main-content">
    <div class="content-header workspace-controls">
        <h1 class="page-title">Hóa đơn đơn hàng</h1>
        <a class="workspace-link" href="<%=base%>/admin-orders">Quản lý đơn hàng</a>
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
    <div class="invoice-layout">
        <form id="invoice-form" method="post" class="workspace-card workspace-form workspace-controls">
            <h2>Thông tin người mua</h2>
            <label>Loại hóa đơn
                <select name="kind"><option value="personal">Cá nhân</option><option value="company">Công ty</option></select>
            </label>
            <label><span id="invoice-name-label">Họ và tên</span><input name="buyer" maxlength="160" required value="<%=Html.escape(order.getUserName())%>"></label>
            <label id="invoice-tax-field" hidden>Mã số thuế công ty<input name="tax" maxlength="30" disabled></label>
            <label>Địa chỉ<textarea name="address" rows="3" maxlength="255" required><%=Html.escape(order.getAddress())%></textarea></label>
            <button class="workspace-button" type="submit" disabled>In hoặc lưu PDF</button>
            <a class="workspace-link" href="<%=base%>/admin-invoice">Chọn đơn khác</a>
        </form>
        <article class="workspace-card invoice-paper" id="invoice-preview">
            <header class="invoice-heading">
                <div><strong>NAM THÀNH FASHION</strong><h2>Hóa đơn bán hàng</h2></div>
                <p>Đơn #<%=order.getId()%><br><span data-invoice="kind">Cá nhân</span>
                    <% if (order.getCreatedDateTime() != null) { %><br>Ngày đặt <%=order.getCreatedDateTime().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))%><% } %>
                </p>
            </header>
            <dl class="invoice-buyer">
                <div><dt>Người mua</dt><dd data-invoice="buyer"><%=Html.escape(order.getUserName())%></dd></div>
                <div data-invoice-tax hidden><dt>Mã số thuế</dt><dd data-invoice="tax"></dd></div>
                <div><dt>Địa chỉ</dt><dd data-invoice="address"><%=Html.escape(order.getAddress())%></dd></div>
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
</body>
</html>
