<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, model.Order, util.Html"%>
<%@ page import="java.text.DecimalFormat, java.text.DecimalFormatSymbols, java.util.Locale"%>
<%@ page import="java.time.format.DateTimeFormatter"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Dashboard Admin | Fashion Store</title>

<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="CSS/admin/Admin.css">
<link rel="stylesheet" href="CSS/admin/admin-dashboard.css?v=20261126">
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
</head>
<body>

    <jsp:include page="sidebarAdmin.jsp">
        <jsp:param name="pageName" value="dashboard" />
    </jsp:include>

    <main class="main-content">

        <div class="content-header">
            <h1 class="page-title">Tổng quan</h1>
            <div class="user-info">
                <i class="fa-solid fa-user-shield" aria-hidden="true"></i>
                <c:set var="dashboardRole" value="${sessionScope.user.isAdmin == 2 ? 'Staff' : 'Admin'}" />
                <c:set var="dashboardName" value="${empty sessionScope.user.fullname ? sessionScope.user.username : sessionScope.user.fullname}" />
                <span><c:out value="${empty dashboardName ? dashboardRole : dashboardName}" /><c:if test="${not empty dashboardName and fn:toLowerCase(fn:trim(dashboardName)) ne fn:toLowerCase(dashboardRole)}"> · ${dashboardRole}</c:if></span>
            </div>
        </div>

        <%
            // Lấy dữ liệu thống kê từ Servlet
            int totalOrders = request.getAttribute("totalOrders") != null ? (int) request.getAttribute("totalOrders") : 0;
            double totalRevenue = request.getAttribute("totalRevenue") != null ? (double) request.getAttribute("totalRevenue") : 0.0;
            int countProcessing = request.getAttribute("countProcessing") != null ? (int) request.getAttribute("countProcessing") : 0;
            int countCancel    = request.getAttribute("countCancel")    != null ? (int) request.getAttribute("countCancel")    : 0;
            int countSuccess   = request.getAttribute("countSuccess")   != null ? (int) request.getAttribute("countSuccess")   : 0;
            int countShipping  = request.getAttribute("countShipping")  != null ? (int) request.getAttribute("countShipping")  : 0;

            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"));
            DecimalFormat number = new DecimalFormat("#,##0", symbols);
            DecimalFormat df = new DecimalFormat("#,##0 '₫'", symbols);
            DateTimeFormatter orderDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        %>

        <section class="dashboard-stats" aria-label="Thống kê đơn hàng">
            <div class="stats-cards">
                <a href="admin-orders" class="card-stat stat-orders">
                    <span class="stat-info">
                        <span class="stat-label">Tổng đơn hàng</span>
                        <strong class="stat-value"><%= number.format(totalOrders) %></strong>
                    </span>
                    <span class="stat-icon" aria-hidden="true"><i class="fa-solid fa-cart-shopping"></i></span>
                </a>

                <a href="${sessionScope.user.isAdmin == 1 ? 'admin-revenue-report' : 'admin-orders?filter=success'}" class="card-stat stat-revenue">
                    <span class="stat-info">
                        <span class="stat-label">Doanh thu thực</span>
                        <strong class="stat-value"><%= number.format(totalRevenue) %> <span class="stat-unit">₫</span></strong>
                    </span>
                    <span class="stat-icon" aria-hidden="true"><i class="fa-solid fa-sack-dollar"></i></span>
                </a>

                <a href="admin-orders?filter=processing" class="card-stat stat-processing">
                    <span class="stat-info">
                        <span class="stat-label">Đang chờ xử lý</span>
                        <strong class="stat-value"><%= number.format(countProcessing) %></strong>
                    </span>
                    <span class="stat-icon" aria-hidden="true"><i class="fa-solid fa-clock"></i></span>
                </a>

                <a href="admin-orders?filter=shipping" class="card-stat stat-shipping">
                    <span class="stat-info">
                        <span class="stat-label">Đang giao hàng</span>
                        <strong class="stat-value"><%= number.format(countShipping) %></strong>
                    </span>
                    <span class="stat-icon" aria-hidden="true"><i class="fa-solid fa-truck-fast"></i></span>
                </a>

                <a href="admin-orders?filter=success" class="card-stat stat-success">
                    <span class="stat-info">
                        <span class="stat-label">Giao thành công</span>
                        <strong class="stat-value"><%= number.format(countSuccess) %></strong>
                    </span>
                    <span class="stat-icon" aria-hidden="true"><i class="fa-solid fa-circle-check"></i></span>
                </a>

                <a href="admin-orders?filter=cancel" class="card-stat stat-cancelled">
                    <span class="stat-info">
                        <span class="stat-label">Đơn bị hủy</span>
                        <strong class="stat-value"><%= number.format(countCancel) %></strong>
                    </span>
                    <span class="stat-icon" aria-hidden="true"><i class="fa-solid fa-ban"></i></span>
                </a>
            </div>
        </section>

        <%-- ===== BIỂU ĐỒ TRÒN TRẠNG THÁI ĐƠN HÀNG ===== --%>
        <div class="charts-section">
            <div class="chart-container donut-card">
                <h4 class="chart-title">
                    <i class="fa-solid fa-chart-pie"></i>
                    Phân tích trạng thái đơn hàng
                </h4>
                <div class="donut-wrapper">
                    <div class="donut-canvas-wrap">
                        <canvas id="orderStatusChart"></canvas>
                    </div>
                    <div class="donut-legend">
                        <div class="legend-item">
                            <span class="legend-dot" style="background: linear-gradient(135deg,#1cc88a,#13855c);"></span>
                            <div>
                                <div class="legend-label">Giao thành công</div>
                                <div class="legend-count" style="color:#1cc88a;"><%= countSuccess %> đơn</div>
                            </div>
                        </div>
                        <div class="legend-item">
                            <span class="legend-dot" style="background: linear-gradient(135deg,#36b9cc,#1a9eac);"></span>
                            <div>
                                <div class="legend-label">Đang giao hàng</div>
                                <div class="legend-count" style="color:#36b9cc;"><%= countShipping %> đơn</div>
                            </div>
                        </div>
                        <div class="legend-item">
                            <span class="legend-dot" style="background: linear-gradient(135deg,#e74a3b,#c0392b);"></span>
                            <div>
                                <div class="legend-label">Đơn đã hủy</div>
                                <div class="legend-count" style="color:#e74a3b;"><%= countCancel %> đơn</div>
                            </div>
                        </div>
                        <div class="legend-item">
                            <span class="legend-dot" style="background: linear-gradient(135deg,#f6c23e,#d4a017);"></span>
                            <div>
                                <div class="legend-label">Đang xử lý</div>
                                <div class="legend-count" style="color:#d4a017;"><%= countProcessing %> đơn</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <% if (request.getParameter("msg") != null) { %>
        <div class="alert-modern">
            <i class="fa-solid fa-circle-check"></i>
            <span>Thao tác thành công! Dữ liệu đã được cập nhật.</span>
        </div>
        <% } %>

        <c:if test="${sessionScope.user.isAdmin == 1}">
        <div class="export-container report-launch-card">
            <div>
                <span class="report-launch-icon"><i class="fa-solid fa-chart-column"></i></span>
                <div>
                    <h3 class="section-title">Báo cáo doanh thu &amp; tồn kho</h3>
                    <p>Xem chi tiết mặt hàng đã bán, chưa bán, tồn kho và xuất Excel theo tháng, 6 tháng hoặc năm.</p>
                </div>
            </div>
            <a href="admin-revenue-report" class="report-launch-button">
                Mở trang báo cáo <i class="fa-solid fa-arrow-right"></i>
            </a>
        </div>
        </c:if>

        <section class="table-container">
            <div class="table-header-box">
                <h2 id="recent-orders-title"><i class="fa-solid fa-list-ul" aria-hidden="true"></i> Đơn hàng gần đây</h2>
            </div>

            <div class="admin-table-scroll recent-orders-scroll" role="region" aria-labelledby="recent-orders-title" tabindex="0">
            <table class="admin-table recent-orders-table">
                <colgroup>
                    <col class="recent-order-id">
                    <col class="recent-order-customer">
                    <col class="recent-order-date">
                    <col>
                    <col class="recent-order-total">
                    <col class="recent-order-status">
                    <col class="recent-order-actions">
                </colgroup>
                <thead>
                    <tr>
                        <th scope="col">Mã đơn</th>
                        <th scope="col">Khách hàng</th>
                        <th scope="col">Ngày đặt</th>
                        <th scope="col">Địa chỉ</th>
                        <th scope="col" class="recent-order-total">Tổng tiền</th>
                        <th scope="col">Trạng thái</th>
                        <th scope="col">Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                    List<Order> list = (List<Order>) request.getAttribute("listOrders");
                    if (list != null && !list.isEmpty()) {
                        for (Order o : list) {
                            String st = o.getStatus();
                            String badgeClass = "bg-process";
                            if (st != null) {
                                if (st.contains("giao") || st.contains("ship")) badgeClass = "bg-shipping";
                                if (st.contains("thành công") || st.contains("HT")) badgeClass = "bg-success";
                                if (st.contains("hủy")) badgeClass = "bg-cancel";
                            } else { st = "Không xác định"; }
                    %>
                    <tr>
                        <td class="recent-order-id"><strong>#<%=o.getId()%></strong></td>
                        <td>
                            <div style="font-weight: 500;"><%=Html.escape(o.getUserName())%></div>
                            <small style="color: #888;"><%=Html.escape(o.getPhoneNumber())%></small>
                        </td>
                        <td class="recent-order-date">
                            <% if (o.getCreatedAt() != null) { %>
                            <time datetime="<%=o.getCreatedAt()%>"><%=orderDate.format(o.getCreatedAt().toLocalDate())%></time>
                            <% } else { %>—<% } %>
                        </td>
                        <td class="recent-order-address">
                            <%=Html.escape(o.getAddress())%>
                        </td>
                        <td class="recent-order-total"><%=df.format(o.getTotalMoney())%></td>
                        <td><span class="badge <%=badgeClass%>"><%=Html.escape(st)%></span></td>
                        <td>
                            <div class="action-group">
                                <a href="order-detail?id=<%=o.getId()%>" class="btn-icon btn-view" title="Xem chi tiết">
                                    <i class="fa-solid fa-eye"></i>
                                </a>
                                <% if ("Đang xử lý".equals(st)) { %>
                                    <form action="update-order" method="post" style="margin: 0;">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="id" value="<%=o.getId()%>">
                                        <input type="hidden" name="action" value="ship">
                                        <button class="btn-icon btn-ship" title="Giao hàng">
                                            <i class="fa-solid fa-truck"></i>
                                        </button>
                                    </form>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                    <% } } else { %>
                    <tr><td colspan="7" style="text-align: center; padding: 40px; color: #999;">
                        <i class="fa-regular fa-folder-open" style="font-size: 40px; margin-bottom: 10px; display: block;"></i>
                        Chưa có đơn hàng nào phát sinh.
                    </td></tr>
                    <% } %>
                </tbody>
            </table>
            </div>
            <jsp:include page="/WEB-INF/views/admin-pagination.jsp" />
        </section>
    </main>

<script>
(function() {
    var success  = <%= countSuccess %>;
    var shipping = <%= countShipping %>;
    var cancel   = <%= countCancel %>;
    var process  = <%= countProcessing %>;

    if (success + shipping + cancel + process === 0) return;

    var ctx = document.getElementById('orderStatusChart').getContext('2d');
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Giao thành công', 'Đang giao', 'Đã hủy', 'Đang xử lý'],
            datasets: [{
                data: [success, shipping, cancel, process],
                backgroundColor: ['#1cc88a', '#36b9cc', '#e74a3b', '#f6c23e'],
                borderColor: '#ffffff',
                borderWidth: 4,
                hoverOffset: 10
            }]
        },
        options: {
            cutout: '68%',
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(30,30,40,0.85)',
                    padding: 12,
                    cornerRadius: 8,
                    callbacks: {
                        label: function(c) {
                            var total = c.dataset.data.reduce(function(a,b){return a+b;},0);
                            var pct = total ? Math.round(c.raw/total*100) : 0;
                            return '  ' + c.label + ': ' + c.raw + ' đơn (' + pct + '%)';
                        }
                    }
                }
            }
        }
    });
})();
</script>
</body>
</html>
