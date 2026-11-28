<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, model.Order, model.User, util.Html"%>
<%@ page import="java.text.DecimalFormat, java.text.DecimalFormatSymbols, java.time.LocalDateTime, java.time.format.DateTimeFormatter, java.util.Locale"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<fmt:setLocale value="${sessionScope.lang != null ? sessionScope.lang : 'vi'}" />
<fmt:setBundle basename="resources.messages" />

<!DOCTYPE html>
<html lang="${sessionScope.lang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <title><fmt:message key="history.page_title" /> | Fashion Store</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="CSS/style.css" />
    <link rel="stylesheet" href="CSS/user/oder-history.css?v=20261126.1" />
</head>
<body class="ym-user-page ym-orders-page">
<header>
    <jsp:include page="header.jsp"><jsp:param name="page" value="#" /></jsp:include>
</header>

<div class="history-container">
    <h1 class="page-title history-title">
        <fmt:message key="history.heading" />
    </h1>

    <%
        List<Order> list = (List<Order>) request.getAttribute("listOrders");
        String activeTab = request.getParameter("tab");
        if (activeTab == null || activeTab.isEmpty()) activeTab = "all";
        DecimalFormat df = new DecimalFormat("#,##0 '₫'", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter hour = DateTimeFormatter.ofPattern("HH:mm");
    %>

    <!-- Tab navigation -->
    <div class="order-tabs">
        <a href="order-history?tab=all" class="tab-item <%= "all".equals(activeTab) ? "active" : "" %>">
            <i class="fa-solid fa-list"></i> <fmt:message key="order.tab.all" />
        </a>
        <a href="order-history?tab=shipping" class="tab-item <%= "shipping".equals(activeTab) ? "active" : "" %>">
            <i class="fa-solid fa-truck"></i> <fmt:message key="order.tab.shipping" />
        </a>
        <a href="order-history?tab=success" class="tab-item <%= "success".equals(activeTab) ? "active" : "" %>">
            <i class="fa-solid fa-circle-check"></i> <fmt:message key="order.tab.success" />
        </a>
        <a href="order-history?tab=pending" class="tab-item <%= "pending".equals(activeTab) ? "active" : "" %>">
            <i class="fa-solid fa-clock"></i> <fmt:message key="order.tab.pending" />
        </a>
        <a href="order-history?tab=cancelled" class="tab-item <%= "cancelled".equals(activeTab) ? "active" : "" %>">
            <i class="fa-solid fa-ban"></i> <fmt:message key="order.tab.cancelled" />
        </a>
    </div>

    <%
        if (list == null || list.isEmpty()) {
    %>
    <div class="empty-cart" style="text-align: center; padding: 50px;">
        <i class="fa-solid fa-box-open" style="font-size: 40px; color: #ccc;"></i>
        <p><fmt:message key="history.empty" /></p>
        <a href="index.jsp" class="btn-shop-now"><fmt:message key="cart.shop_now" /></a>
    </div>
    <%
    } else {
        List<Order> filteredList = new java.util.ArrayList<>();
        for (Order o : list) {
            String normalized = o.getNormalizedStatus();
            boolean include = false;
            switch (activeTab) {
                case "shipping":
                    include = model.Order.STATUS_SHIPPING.equals(normalized);
                    break;
                case "success":
                    include = model.Order.STATUS_SUCCESS.equals(normalized);
                    break;
                case "pending":
                    include = model.Order.STATUS_PROCESSING.equals(normalized)
                            || model.Order.STATUS_PENDING_MOMO.equals(normalized)
                            || model.Order.STATUS_PENDING_VNPAY.equals(normalized)
                            || model.Order.STATUS_PAID_PROCESSING.equals(normalized);
                    break;
                case "cancelled":
                    include = model.Order.STATUS_CANCELLED.equals(normalized)
                            || model.Order.STATUS_MOMO_FAILED.equals(normalized)
                            || model.Order.STATUS_VNPAY_FAILED.equals(normalized);
                    break;
                default:
                    include = true;
                    break;
            }
            if (include) filteredList.add(o);
        }

        if (filteredList.isEmpty()) {
    %>
    <div class="empty-cart" style="text-align: center; padding: 50px;">
        <i class="fa-solid fa-box-open" style="font-size: 40px; color: #ccc;"></i>
        <p><fmt:message key="order.empty_tab" /></p>
        <a href="index.jsp" class="btn-shop-now"><fmt:message key="cart.shop_now" /></a>
    </div>
    <%
    } else {
    %>
    <table class="order-table history-orders-table" role="table">
        <colgroup>
            <col class="history-col-id">
            <col class="history-col-placed">
            <col class="history-col-delivered">
            <col>
            <col class="history-col-total">
            <col class="history-col-status">
            <col class="history-col-action">
        </colgroup>
        <thead role="rowgroup">
        <tr role="row">
            <th role="columnheader" scope="col"><fmt:message key="history.id" /></th>
            <th role="columnheader" scope="col"><fmt:message key="history.date" /></th>
            <th role="columnheader" scope="col"><fmt:message key="order.delivered_at" /></th>
            <th role="columnheader" scope="col"><fmt:message key="history.address" /></th>
            <th role="columnheader" scope="col" class="history-total"><fmt:message key="history.total" /></th>
            <th role="columnheader" scope="col"><fmt:message key="history.status" /></th>
            <th role="columnheader" scope="col"><fmt:message key="history.action" /></th>
        </tr>
        </thead>
        <tbody role="rowgroup">
        <%
            for (Order o : filteredList) {
                String statusClass;
                String normalized = o.getNormalizedStatus();
                if (model.Order.STATUS_CANCELLED.equals(normalized)
                        || model.Order.STATUS_MOMO_FAILED.equals(normalized)
                        || model.Order.STATUS_VNPAY_FAILED.equals(normalized)) {
                    statusClass = "status-cancelled";
                } else if (model.Order.STATUS_SUCCESS.equals(normalized)) {
                    statusClass = "status-success";
                } else if (model.Order.STATUS_SHIPPING.equals(normalized)) {
                    statusClass = "status-shipped";
                } else {
                    statusClass = "status-processing";
                }
        %>
        <tr role="row">
            <td role="cell" class="history-id"><strong>#<%=o.getId()%></strong></td>
            <td role="cell" class="history-placed">
                <span class="history-cell-label" aria-hidden="true"><fmt:message key="history.date" /></span>
                <% LocalDateTime placedAt = o.getCreatedDateTime(); if (placedAt != null) { %>
                <time class="history-date-time" datetime="<%=placedAt%>"><%=date.format(placedAt)%><small><%=hour.format(placedAt)%></small></time>
                <% } else { %>—<% } %>
            </td>
            <td role="cell" class="history-delivered">
                <span class="history-cell-label" aria-hidden="true"><fmt:message key="order.delivered_at" /></span>
                <% if (o.isSuccessStatus()) { %>
                <span class="order-delivery-time">
                    <% if (o.getDeliveredAt() != null) { %>
                    <time class="history-date-time" datetime="<%=o.getDeliveredAt().toInstant()%>"><fmt:formatDate value="<%=o.getDeliveredAt()%>" pattern="dd/MM/yyyy" timeZone="Asia/Ho_Chi_Minh" /><small><fmt:formatDate value="<%=o.getDeliveredAt()%>" pattern="HH:mm" timeZone="Asia/Ho_Chi_Minh" /></small></time>
                    <% } else { %><fmt:message key="order.delivered_at_unrecorded" /><% } %>
                </span>
                <% } else { %>—<% } %>
            </td>
            <% String address = Html.escape(o.getAddress()); %>
            <td role="cell" class="history-address">
                <span class="history-cell-label" aria-hidden="true"><fmt:message key="history.address" /></span>
                <span class="history-address-text" title="<%=address%>"><%=address.isBlank() ? "—" : address%></span>
            </td>
            <td role="cell" class="history-total">
                <span class="history-cell-label" aria-hidden="true"><fmt:message key="history.total" /></span>
                <strong class="history-amount"><%=df.format(o.getTotalMoney())%></strong>
            </td>
            <td role="cell" class="history-status"><span class="status-tag <%=statusClass%>"><% String statusKey = o.getStatusKey(); if (statusKey != null) { %><fmt:message key="<%=statusKey%>" /><% } else { %><%=Html.escape(o.getDisplayStatus())%><% } %></span></td>
            <td role="cell" class="history-action">
                <a href="order-detail?id=<%=o.getId()%>" class="btn-view-detail">
                    <fmt:message key="history.view_detail" />
                </a>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <%
            }
        }
    %>
</div>

<footer>
    <jsp:include page="footer.jsp" />
</footer>
</body>
</html>
