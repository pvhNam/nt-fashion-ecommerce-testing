<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, model.Order, util.Html"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="java.text.DecimalFormatSymbols, java.time.LocalDateTime, java.time.format.DateTimeFormatter, java.util.Locale"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Quản Lý Đơn Hàng | Admin</title>

<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="CSS/admin/Admin.css">
<link rel="stylesheet" href="CSS/admin/AdminOrder.css?v=20261126.1">
</head>
<body>

    <jsp:include page="sidebarAdmin.jsp">
        <jsp:param name="pageName" value="orders" />
    </jsp:include>

    <main class="main-content">
        <%
            String filterParam = (String) request.getAttribute("filter");
            List<Order> list = (List<Order>) request.getAttribute("listOrders");
            DecimalFormat money = new DecimalFormat("#,##0 '₫'",
                    DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
            DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/uuuu");
            DateTimeFormatter hour = DateTimeFormatter.ofPattern("HH:mm");
        %>
        <div class="content-header">
            <h1 class="page-title">Quản Lý Đơn Hàng</h1>
            <form action="admin-orders" method="get" class="admin-filter-select" data-admin-ajax>
                <label>
                    <i class="fa-solid fa-filter" aria-hidden="true"></i>
                    <span>Trạng thái</span>
                    <select name="filter" data-admin-auto-submit>
                        <option value="" <%= filterParam == null || filterParam.isEmpty() ? "selected" : "" %>>Tất cả đơn hàng</option>
                        <option value="processing" <%= "processing".equals(filterParam) ? "selected" : "" %>>Đang xử lý</option>
                        <option value="shipping" <%= "shipping".equals(filterParam) ? "selected" : "" %>>Đang giao hàng</option>
                        <option value="success" <%= "success".equals(filterParam) ? "selected" : "" %>>Giao thành công</option>
                        <option value="cancel" <%= "cancel".equals(filterParam) ? "selected" : "" %>>Đã hủy / thất bại</option>
                    </select>
                </label>
            </form>
        </div>

        <% if (request.getParameter("msg") != null) { %>
        <div class="alert alert-success">
            <i class="fa-solid fa-check-circle"></i>
            <span>Thao tác thành công!</span>
        </div>
        <% } %>

        <%
            String filterName = null;
            String filterColor = "#4e73df";

            if ("processing".equals(filterParam)) {
                filterName = "Đang xử lý";
                filterColor = "#f6c23e";
            } else if ("shipping".equals(filterParam)) {
                filterName = "Đang giao hàng";
                filterColor = "#36b9cc";
            } else if ("cancel".equals(filterParam)) {
                filterName = "Đã hủy / thất bại";
                filterColor = "#e74a3b";
            } else if ("success".equals(filterParam)) {
                filterName = "Giao thành công";
                filterColor = "#1cc88a";
            }
        %>
        <% if (filterName != null) { %>
        <div class="filter-bar">
            <span class="filter-tag" style="border-color: <%=filterColor%>; color: <%=filterColor%>;">
                <i class="fa-solid fa-filter"></i>
                Lọc: <strong><%=filterName%></strong>
                · ${adminTotalRows} đơn
            </span>
            <a href="admin-orders" class="btn-clear-filter" data-admin-ajax-link>
                <i class="fa-solid fa-xmark"></i> Xem tất cả
            </a>
        </div>
        <% } %>

        <div class="card-box">
            <div class="admin-table-scroll order-table-scroll" role="region" aria-label="Danh sách đơn hàng" tabindex="0">
            <table class="admin-table order-table" role="table">
                <colgroup>
                    <col class="order-col-id">
                    <col class="order-col-customer">
                    <col class="order-col-date">
                    <col class="order-col-delivery">
                    <col>
                    <col class="order-col-total">
                    <col class="order-col-payment">
                    <col class="order-col-status">
                    <col class="order-col-actions">
                </colgroup>
                <thead role="rowgroup">
                    <tr role="row">
                        <th role="columnheader" scope="col">Mã đơn</th>
                        <th role="columnheader" scope="col">Khách hàng</th>
                        <th role="columnheader" scope="col">Ngày đặt</th>
                        <th role="columnheader" scope="col">Ngày nhận</th>
                        <th role="columnheader" scope="col">Địa chỉ</th>
                        <th role="columnheader" scope="col" class="order-total">Tổng tiền</th>
                        <th role="columnheader" scope="col">Thanh toán</th>
                        <th role="columnheader" scope="col">Trạng thái</th>
                        <th role="columnheader" scope="col">Thao tác</th>
                    </tr>
                </thead>
                <tbody role="rowgroup">
                    <%
                    if (list != null && !list.isEmpty()) {
                        for (Order order : list) {
                            LocalDateTime placedAt = order.getCreatedDateTime();
                            String customer = Html.escape(order.getUserName());
                            String phone = Html.escape(order.getPhoneNumber());
                            String address = Html.escape(order.getAddress());
                    %>
                    <tr role="row" class="order-entry">
                        <td role="cell" class="order-id"><strong>#<%=order.getId()%></strong></td>

                        <td role="cell" class="order-customer-cell">
                            <span class="order-cell-label" aria-hidden="true">Khách hàng</span>
                            <div class="order-customer" title="<%=customer%>"><%=customer.isBlank() ? "—" : customer%></div>
                            <small class="order-phone" title="<%=phone%>"><%=phone%></small>
                        </td>

                        <td role="cell" class="order-date">
                            <span class="order-cell-label" aria-hidden="true">Ngày đặt</span>
                            <% if (placedAt != null) { %>
                            <time class="order-date-time" datetime="<%=placedAt%>"><%=date.format(placedAt)%> <small><%=hour.format(placedAt)%></small></time>
                            <% } else { %>—<% } %>
                        </td>

                        <td role="cell" class="order-delivery-date">
                            <span class="order-cell-label" aria-hidden="true">Ngày nhận</span>
                            <% if (order.isSuccessStatus()) { %>
                            <span class="order-delivery-time">
                                <% if (order.getDeliveredAt() != null) { %>
                                <time class="order-date-time" datetime="<%=order.getDeliveredAt().toInstant()%>"><fmt:formatDate value="<%=order.getDeliveredAt()%>" pattern="dd/MM/yyyy" timeZone="Asia/Ho_Chi_Minh" /> <small><fmt:formatDate value="<%=order.getDeliveredAt()%>" pattern="HH:mm" timeZone="Asia/Ho_Chi_Minh" /></small></time>
                                <% } else { %>Chưa ghi nhận<% } %>
                            </span>
                            <% } else { %>—<% } %>
                        </td>

                        <td role="cell" class="order-address">
                            <span class="order-cell-label" aria-hidden="true">Địa chỉ</span>
                            <span class="order-address-text" title="<%=address%>"><%=address.isBlank() ? "—" : address%></span>
                        </td>

                        <td role="cell" class="order-total"><span class="order-cell-label" aria-hidden="true">Tổng tiền</span><%=money.format(order.getTotalMoney())%></td>
                        <td role="cell" class="order-payment"><span class="order-cell-label" aria-hidden="true">Thanh toán</span><%=order.getPaymentMethod() == null || order.getPaymentMethod().isBlank()
                                ? "—" : Html.escape(order.getPaymentMethod())%></td>
                        <td role="cell" class="order-status"><span class="badge <%=order.getAdminBadgeClass()%>"><%=order.getDisplayStatus()%></span></td>

                        <td role="cell" class="order-actions">
                            <div class="action-group">
                                <a href="order-detail?id=<%=order.getId()%>" class="btn-action btn-view"
                                   aria-label="Xem chi tiết đơn #<%=order.getId()%>" title="Xem chi tiết">
                                    <i class="fa-solid fa-eye"></i>
                                </a>

                                <% if (order.isShippableStatus()) { %>
                                <form action="update-order" method="post" style="margin: 0;">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="id" value="<%=order.getId()%>">
                                    <input type="hidden" name="action" value="ship">
                                    <button type="submit" class="btn-action btn-ship"
                                            aria-label="Chuyển đơn #<%=order.getId()%> sang đang giao" title="Chuyển sang đang giao">
                                        <i class="fa-solid fa-truck"></i>
                                    </button>
                                </form>
                                <% } %>

                                <% if (order.isAdminCancelableStatus()) { %>
                                <form action="update-order" method="post" style="margin: 0;">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="id" value="<%=order.getId()%>">
                                    <input type="hidden" name="action" value="cancel">
                                    <button type="submit" class="btn-action btn-cancel"
                                            aria-label="Hủy đơn #<%=order.getId()%>" title="Hủy đơn hàng"
                                            onclick="return confirm('Bạn có chắc chắn muốn hủy đơn hàng này?')">
                                        <i class="fa-solid fa-xmark"></i>
                                    </button>
                                </form>
                                <% } %>

                                <% if (order.isShippingStatus()) { %>
                                <form action="update-order" method="post" style="margin: 0;">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="id" value="<%=order.getId()%>">
                                    <input type="hidden" name="action" value="success">
                                    <button type="submit" class="btn-action btn-success"
                                            aria-label="Xác nhận giao thành công đơn #<%=order.getId()%>" title="Xác nhận giao thành công">
                                        <i class="fa-solid fa-check"></i>
                                    </button>
                                </form>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                    <%
                        }
                    } else {
                    %>
                    <tr role="row" class="order-empty-row">
                        <td role="cell" colspan="9" style="text-align: center; padding: 40px; color: #888;">
                            <i class="fa-solid fa-box-open" style="font-size: 40px; margin-bottom: 10px; display: block;"></i>
                            <%=filterName == null ? "Chưa có đơn hàng nào trong hệ thống." : "Không có đơn hàng phù hợp với bộ lọc."%>
                        </td>
                    </tr>
                    <% } %>
                </tbody>
            </table>
            </div>
            <jsp:include page="/WEB-INF/views/admin-pagination.jsp" />
        </div>
    </main>
</body>
</html>
