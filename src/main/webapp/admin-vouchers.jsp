<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, model.Voucher, util.Html"%>
<%@ page import="java.text.DecimalFormat"%>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Quản lý Voucher | Fashion Store</title>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="CSS/admin/Admin.css">
<link rel="stylesheet" href="CSS/admin/admin-vouchers.css">
</head>
<body>
    <jsp:include page="sidebarAdmin.jsp">
        <jsp:param name="pageName" value="vouchers" />
    </jsp:include>

    <main class="main-content">

        <div class="content-header">
            <div>
                <h1 class="page-title">Quản lý Mã Giảm Giá</h1>
                <%
                List<Voucher> listV = (List<Voucher>) request.getAttribute("listVouchers");
                int total = (listV != null) ? listV.size() : 0;
                %>
                <p class="voucher-subtitle"><%= total %> voucher đang hoạt động</p>
            </div>
            <a href="admin-vouchers?action=add" class="btn-add-new">
                <i class="fa-solid fa-plus"></i> Thêm Voucher
            </a>
        </div>

        <% String msg = request.getParameter("msg");
           if ("error_value_range".equals(msg)) { %>
        <div class="alert alert-danger" role="alert">
            <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
            Giá trị giảm và đơn hàng tối thiểu phải là số hợp lệ, không âm. Mức giảm theo phần trăm phải từ 0 đến 100%.
        </div>
        <% } else if ("error_expiry_date".equals(msg)) { %>
        <div class="alert alert-danger" role="alert">
            <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
            Ngày hết hạn phải đúng định dạng yyyy-MM-dd, là ngày hợp lệ và có năm từ 1000 đến 9999.
        </div>
        <% } else if ("error_action".equals(msg)) { %>
        <div class="alert alert-danger" role="alert">
            <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
            Thao tác voucher không hợp lệ. Vui lòng thực hiện lại từ trang quản lý voucher.
        </div>
        <% } else if ("error_code_required".equals(msg)) { %>
        <div class="alert alert-danger" role="alert">
            <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
            Vui lòng nhập mã voucher, không được để trống hoặc chỉ chứa khoảng trắng.
        </div>
        <% } else if ("error_discount_type".equals(msg)) { %>
        <div class="alert alert-danger" role="alert">
            <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
            Vui lòng chọn loại giảm giá Tiền mặt hoặc Phần trăm.
        </div>
        <% } else if ("success".equals(msg) || "deleted".equals(msg)) { %>
        <div class="alert alert-success">
            <i class="fa-solid fa-circle-check"></i> Thao tác thành công!
        </div>
        <% } %>

        <%
        DecimalFormat df = new DecimalFormat("#,### VNĐ");
        if (listV != null && !listV.isEmpty()) {
        %>
        <div class="voucher-grid">
            <% for (Voucher v : listV) {
                boolean isPercent = "PERCENT".equals(v.getDiscountType());
                String discountLabel = isPercent ? (int)v.getDiscountAmount() + "%" : df.format(v.getDiscountAmount());
                String cardAccent = isPercent ? "accent-purple" : "accent-green";
            %>
            <div class="voucher-card <%=cardAccent%>">
                <div class="vc-top">
                    <div class="vc-code">
                        <i class="fa-solid fa-ticket"></i>
                        <span><%=Html.escape(v.getCode())%></span>
                    </div>
                    <div class="vc-discount"><%=discountLabel%></div>
                </div>

                <div class="vc-divider">
                    <span class="vc-cut left"></span>
                    <div class="vc-dash"></div>
                    <span class="vc-cut right"></span>
                </div>

                <div class="vc-bottom">
                    <div class="vc-info-row">
                        <i class="fa-solid fa-circle-info"></i>
                        <span><%=Html.escape(v.getDescription() != null && !v.getDescription().isEmpty() ? v.getDescription() : "Không có mô tả")%></span>
                    </div>
                    <div class="vc-info-row">
                        <i class="fa-solid fa-cart-shopping"></i>
                        <span>Đơn tối thiểu: <strong><%=df.format(v.getMinOrder())%></strong></span>
                    </div>
                    <div class="vc-info-row">
                        <i class="fa-regular fa-calendar-xmark"></i>
                        <span>Hết hạn: <strong><%=v.getExpiryDate()%></strong></span>
                    </div>

                    <div class="vc-actions">
                        <a href="admin-vouchers?action=edit&id=<%=v.getId()%>" class="vc-btn vc-edit">
                            <i class="fa-solid fa-pen"></i> Sửa
                        </a>
                        <form action="admin-vouchers" method="post" style="display:inline;margin:0"
                              onsubmit="return confirm('Xóa voucher <%=Html.js(v.getCode())%>?')">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="<%=v.getId()%>">
                            <button type="submit" class="vc-btn vc-delete" style="border:none;cursor:pointer;font:inherit">
                                <i class="fa-solid fa-trash"></i> Xóa
                            </button>
                        </form>
                    </div>
                </div>
            </div>
            <% } %>
        </div>

        <% } else { %>
        <div class="empty-state">
            <i class="fa-solid fa-ticket-simple"></i>
            <h3>Chưa có voucher nào</h3>
            <p>Tạo voucher đầu tiên để bắt đầu khuyến mãi</p>
            <a href="admin-vouchers?action=add" class="btn-add-new" style="margin-top:12px;">
                <i class="fa-solid fa-plus"></i> Tạo Voucher
            </a>
        </div>
        <% } %>

    </main>
</body>
</html>
