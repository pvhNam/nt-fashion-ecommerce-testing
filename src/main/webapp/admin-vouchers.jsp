<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, model.Voucher, util.Html"%>
<%@ page import="java.text.DecimalFormat, java.text.DecimalFormatSymbols, java.util.Locale"%>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Quản lý Voucher | Fashion Store</title>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="CSS/admin/Admin.css">
<link rel="stylesheet" href="CSS/admin/admin-vouchers.css?v=20261125">
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
                <p class="voucher-subtitle"><%= total %> voucher</p>
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
        <% } else if ("success".equals(msg) || "deleted".equals(msg)) { %>
        <div class="alert alert-success">
            <i class="fa-solid fa-circle-check"></i> Thao tác thành công!
        </div>
        <% } %>

        <%
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"));
        DecimalFormat df = new DecimalFormat("#,##0 '₫'", symbols);
        DecimalFormat percent = new DecimalFormat("0.##'%'", symbols);
        if (listV != null && !listV.isEmpty()) {
        %>
        <div class="voucher-grid">
            <% for (Voucher v : listV) {
                boolean isPercent = "PERCENT".equals(v.getDiscountType());
                String discountLabel = isPercent ? percent.format(v.getDiscountAmount()) : df.format(v.getDiscountAmount());
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
                        <span>Hết hạn: <strong><%=v.getExpiryDate() == null ? "Không giới hạn" : v.getExpiryDate()%></strong></span>
                    </div>

                    <div class="vc-actions">
                        <a href="admin-vouchers?action=edit&id=<%=v.getId()%>" class="vc-btn vc-edit">
                            <i class="fa-solid fa-pen"></i> Sửa
                        </a>
                        <form action="admin-vouchers" method="post"
                              onsubmit="return confirm('Xóa voucher <%=Html.js(v.getCode())%>?')">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="<%=v.getId()%>">
                            <button type="submit" class="vc-btn vc-delete">
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
