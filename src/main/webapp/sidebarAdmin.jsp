<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="model.User"%>
<%
String activePage = request.getParameter("pageName");
if (activePage == null)
	activePage = "";

// Xử lý Phân quyền
User currUser = (User) session.getAttribute("user");
int role = currUser != null ? currUser.getIsAdmin() : 0;
%>

<link rel="stylesheet" href="CSS/admin/sidebar.css">
<meta name="csrf-token" content="${csrfToken}">
<link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/shared/smooth-navigation.css?v=20260822.4">
<script src="${pageContext.request.contextPath}/JS/smooth-navigation.js?v=20261027.1" defer></script>

<div class="sidebar">
	<div class="sidebar-brand">
		<i class="fa-solid fa-shield-halved" style="margin-right: 10px;"></i>
		ADMIN PANEL
	</div>

	<div class="sidebar-menu">
		<a href="admin"
			class="menu-item <%=activePage.equals("dashboard") ? "active" : ""%>">
			<i class="fa-solid fa-chart-pie"></i> Tổng quan
		</a>
		<%-- Staff chỉ thấy đơn hàng, sản phẩm, nhập kho (KAN-11: IS-02, khớp AdminAuthFilter) --%>
		<%
		if (role == 1) {
		%>
		<a href="admin-revenue-report"
			class="menu-item <%=activePage.equals("revenue-report") ? "active" : ""%>">
			<i class="fa-solid fa-chart-column"></i> Báo cáo doanh thu
		</a>
		<%
		}
		%>
		<a href="admin-orders"
			class="menu-item <%=activePage.equals("orders") ? "active" : ""%>">
			<i class="fa-solid fa-box"></i> Quản lý Đơn hàng
		</a> <a href="admin-products"
			class="menu-item <%=activePage.equals("products") ? "active" : ""%>">
			<i class="fa-solid fa-shirt"></i> Quản lý Sản phẩm
		</a>
		<a href="admin-stock-import"
			class="menu-item <%=activePage.equals("stock-import") ? "active" : ""%>">
			<i class="fa-solid fa-truck-ramp-box"></i> Đặt hàng nhập kho
		</a>
		<%
		if (role == 1) {
		%>
		<a href="admin-collections" class="menu-item <%=activePage.equals("collections") ? "active" : ""%>">
			<i class="fa-solid fa-layer-group"></i> Quản lý Bộ sưu tập
		</a>
		<a href="admin-users"
			class="menu-item <%=activePage.equals("users") ? "active" : ""%>">
			<i class="fa-solid fa-user-group"></i> Quản lý Khách hàng
		</a>
		<a href="admin-staffs"
			class="menu-item <%=activePage.equals("staffs") ? "active" : ""%>">
			<i class="fa-solid fa-address-book"></i> Danh sách Nhân viên
		</a>
		 <a href="admin-news"
			class="menu-item <%=activePage.equals("news") ? "active" : ""%>">
			<i class="fa-solid fa-newspaper"></i> Quản lý Tin tức
		</a> <a href="admin-vouchers"
			class="menu-item <%=activePage.equals("vouchers") ? "active" : ""%>">
			<i class="fa-solid fa-ticket"></i> Quản lý Voucher
		</a>
		<%
		}
		%>
	</div>

	<div class="sidebar-footer" style="margin-top: auto; padding: 20px;">
		<a href="logout"
			style="color: #ccc; text-decoration: none; display: flex; align-items: center; gap: 10px; transition: 0.3s;">
			<i class="fa-solid fa-right-from-bracket"></i> Đăng xuất
		</a>
	</div>
</div>
