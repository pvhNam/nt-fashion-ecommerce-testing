<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, model.Product, util.Html"%>
<%@ page import="java.text.DecimalFormat, java.text.DecimalFormatSymbols, java.util.Locale"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản Lý Sản Phẩm | Admin</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="CSS/admin/Admin.css">
    <link rel="stylesheet" href="CSS/admin/AdminProduct.css?v=20261126">
</head>
<body>

<jsp:include page="sidebarAdmin.jsp">
    <jsp:param name="pageName" value="products" />
</jsp:include>

<main class="main-content products-page">
    <div class="content-header">
        <h1 class="page-title">Quản Lý Sản Phẩm</h1>
        <div class="admin-header-tools">
            <form action="admin-products" method="get" class="admin-filter-search" data-admin-ajax>
                <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
                <input type="search" name="search" value="<c:out value='${searchKeyword}' />"
                       placeholder="Tìm tên hoặc mã sản phẩm..." aria-label="Tìm sản phẩm" data-admin-live-search>
                <button type="submit" title="Tìm kiếm"><i class="fa-solid fa-arrow-right"></i></button>
            </form>
            <a href="admin-stock-import" class="btn-add-new product-header-import">
                <i class="fa-solid fa-truck-ramp-box" aria-hidden="true"></i> Đặt hàng nhập kho
            </a>
            <a href="admin-products?type=add" class="btn-add-new">
                <i class="fa-solid fa-plus" aria-hidden="true"></i> Thêm sản phẩm mới
            </a>
        </div>
    </div>

    <% if (request.getParameter("msg") != null) {
        String msg = request.getParameter("msg");
        String alertClass = "alert-success";
        String text = "Thao tác thành công!";
        if(msg.equals("error_data")) {
            alertClass = "alert-danger";
            text = "Dữ liệu không hợp lệ. Giá phải là số hữu hạn không âm; tồn kho phải là số nguyên từ 0 đến 2147483647. Vui lòng kiểm tra các trường bắt buộc.";
        }
        else if(msg.equals("error_update")) {
            alertClass = "alert-danger";
            text = "Không thể cập nhật sản phẩm. Thay đổi sản phẩm và ảnh phụ chưa được lưu.";
        }
        else if(msg.equals("error_delete_referenced")) {
            alertClass = "alert-danger";
            text = "Không thể xóa vì sản phẩm đang được sử dụng, ví dụ trong giỏ hàng.";
        }
        else if(msg.equals("error_delete_not_found") || msg.equals("error_delete_id")) {
            alertClass = "alert-danger";
            text = "Không tìm thấy sản phẩm cần xóa.";
        }
        else if(msg.equals("error_delete")) {
            alertClass = "alert-danger";
            text = "Không thể xóa sản phẩm. Vui lòng thử lại.";
        }
        else if(msg.contains("error")) { alertClass = "alert-danger"; text = "Có lỗi xảy ra!"; }
        else if(msg.equals("deleted")) text = "Đã xóa sản phẩm.";
        else if(msg.equals("imported")) text = "Đã nhập hàng vào kho thành công!";
    %>
    <div class="alert <%=alertClass%>" role="<%="alert-danger".equals(alertClass) ? "alert" : "status"%>">
        <i class="fa-solid <%="alert-danger".equals(alertClass) ? "fa-circle-exclamation" : "fa-circle-check"%>" aria-hidden="true"></i> <%= text %>
    </div>
    <% } %>

    <div class="card-box">
        <div class="admin-table-scroll product-table-scroll" role="region" aria-label="Danh sách sản phẩm" tabindex="0">
        <table data-admin-datatable class="admin-table product-table">
            <thead>
            <tr>
                <th scope="col" class="product-col-id">Mã SP</th>
                <th scope="col" class="product-col-image">Hình ảnh</th>
                <th scope="col">Tên sản phẩm</th>
                <th scope="col" class="product-col-price">Giá bán</th>
                <th scope="col" class="product-col-stock">Tồn kho</th>
                <th scope="col" class="product-col-actions">Thao tác</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<Product> list = (List<Product>) request.getAttribute("listP");
                DecimalFormat df = new DecimalFormat("#,##0 '₫'",
                        DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN")));

                if (list != null && !list.isEmpty()) {
                    for (Product p : list) {
            %>
            <tr>
                <td><strong>#<%=p.getPid()%></strong></td>
                <td>
                    <span class="admin-product-thumb" role="img" aria-label="Ảnh sản phẩm">
                        <i class="fa-regular fa-image" aria-hidden="true"></i>
                        <% if (p.getImage() != null && !p.getImage().isBlank()) { %>
                        <img src="<%=Html.escape(p.getImage())%>" alt="" onerror="this.hidden = true">
                        <% } %>
                    </span>
                </td>

                <td>
                    <a href="admin-variants?pid=<%=p.getPid()%>"
                       class="product-name" title="Xem chi tiết kho theo màu và kích cỡ">
                        <%=Html.escape(p.getPdescription())%>
                        <i class="fa-solid fa-arrow-up-right-from-square" aria-hidden="true"></i>
                    </a>
                </td>

                <td class="product-col-price"><%=df.format(p.getPrice())%></td>
                <td class="product-col-stock">
                    <span class="stock-badge">
                        <%=p.getStockquantyti()%>
                    </span>
                </td>
                <td>
                    <div class="action-group">
                        <a href="admin-stock-import?pid=<%=p.getPid()%>" class="btn-icon btn-import"
                           title="Nhập thêm hàng" aria-label="Nhập thêm hàng cho sản phẩm #<%=p.getPid()%>">
                            <i class="fa-solid fa-box-open" aria-hidden="true"></i>
                        </a>

                        <a href="admin-products?type=edit&pid=<%=p.getPid()%>" class="btn-icon btn-edit"
                           title="Sửa thông tin" aria-label="Sửa thông tin sản phẩm #<%=p.getPid()%>">
                            <i class="fa-solid fa-pen" aria-hidden="true"></i>
                        </a>

                        <form action="admin-products" method="post"
                              onsubmit="return confirm('Bạn có chắc chắn muốn xóa?')">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="pid" value="<%=p.getPid()%>">
                            <button type="submit" class="btn-icon btn-delete" title="Xóa sản phẩm"
                                    aria-label="Xóa sản phẩm #<%=p.getPid()%>">
                                <i class="fa-solid fa-trash" aria-hidden="true"></i>
                            </button>
                        </form>
                    </div>
                </td>
            </tr>
            <% } } else { %>
            <tr><td colspan="6" style="text-align: center; padding: 40px; color: #777;"><i class="fa-solid fa-box-open" style="font-size:30px; color:#ccc; margin-bottom:10px;"></i><br>Chưa có sản phẩm nào.</td></tr>
            <% } %>
            </tbody>
        </table>
        </div>
        <jsp:include page="/WEB-INF/views/admin-pagination.jsp" />
    </div>
</main>
</body>
</html>
