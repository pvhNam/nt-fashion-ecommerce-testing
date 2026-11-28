<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.CampaignDAO.Campaign, java.util.List, java.time.LocalDate, java.time.format.DateTimeFormatter, model.Voucher, util.Html" %>
<%
    boolean admin = Boolean.TRUE.equals(request.getAttribute("adminCampaigns"));
    List<Campaign> campaigns = (List<Campaign>) request.getAttribute("campaigns");
    List<Voucher> vouchers = (List<Voucher>) request.getAttribute("campaignVouchers");
    Campaign selected = (Campaign) request.getAttribute("selectedCampaign");
    String base = request.getContextPath();
    LocalDate today = LocalDate.now();
    DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/uuuu");
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chiến dịch khuyến mãi | Nam Thành</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <% if (!admin) { %><link rel="stylesheet" href="<%=base%>/CSS/style.css"><% } %>
    <link rel="stylesheet" href="<%=base%>/CSS/shared/customer-tools.css?v=20261126">
    <% if (admin) { %><link rel="stylesheet" href="<%=base%>/CSS/admin/Admin.css"><% } %>
</head>
<body class="<%=admin ? "workspace-admin" : "workspace-public"%>">
<% if (admin) { %>
<jsp:include page="/sidebarAdmin.jsp"><jsp:param name="pageName" value="campaigns" /></jsp:include>
<% } else { %><jsp:include page="/header.jsp"><jsp:param name="page" value="campaigns" /></jsp:include><% } %>
<main class="<%=admin ? "main-content" : "workspace-main"%>">
    <header class="workspace-heading"><h1>Chiến dịch khuyến mãi</h1></header>
    <% if (admin && "1".equals(request.getParameter("saved"))) { %><p class="workspace-notice" role="status">Đã lưu chiến dịch.</p><% } %>
    <% if (!admin) {
        String claim = request.getParameter("claim");
        if ("saved".equals(claim)) { %><p class="workspace-notice" role="status">Đã lưu mã vào ví voucher.</p><% }
        else if ("exists".equals(claim)) { %><p class="workspace-notice" role="status">Mã này đã có trong ví của bạn.</p><% }
        else if ("unavailable".equals(claim)) { %><p class="workspace-notice" role="status">Mã ưu đãi hiện không còn khả dụng.</p><% }
    } %>
    <div class="<%=admin ? "workspace-columns campaign-admin-columns" : "campaign-public-grid"%>">
        <% if (admin) { %>
        <section class="workspace-card">
            <h2><%=selected == null ? "Tạo chiến dịch" : "Chỉnh sửa chiến dịch"%></h2>
            <form class="workspace-form" method="post" action="<%=base%>/admin-campaigns">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <% if (selected != null) { %><input type="hidden" name="id" value="<%=selected.id()%>"><% } %>
                <label>Tên chiến dịch<input name="title" required maxlength="160" value="<%=selected == null ? "" : Html.escape(selected.title())%>"></label>
                <label>Nội dung<textarea name="description" required maxlength="2000" rows="4"><%=selected == null ? "" : Html.escape(selected.description())%></textarea></label>
                <div class="workspace-date-fields">
                    <label>Bắt đầu<input type="date" name="start" min="1000-01-01" max="9999-12-31" required value="<%=selected == null ? today : selected.start()%>"></label>
                    <label>Kết thúc<input type="date" name="end" min="1000-01-01" max="9999-12-31" required value="<%=selected == null ? today : selected.end()%>"></label>
                </div>
                <label>Voucher công khai
                    <select name="voucherId">
                        <option value="">Không gắn voucher</option>
                        <% for (Voucher item : vouchers) { %>
                        <option value="<%=item.getId()%>" <%=selected != null && Integer.valueOf(item.getId()).equals(selected.voucherId()) ? "selected" : ""%>><%=Html.escape(item.getCode())%></option>
                        <% } %>
                    </select>
                </label>
                <label class="workspace-checkbox"><input type="checkbox" name="published" <%=selected != null && selected.published() ? "checked" : ""%>> Công khai trong thời gian chạy</label>
                <button class="workspace-button" type="submit">Lưu chiến dịch</button>
                <% if (selected != null) { %><a class="workspace-link" href="<%=base%>/admin-campaigns">Tạo chiến dịch mới</a><% } %>
            </form>
        </section>
        <% } %>
        <section class="<%=admin ? "workspace-card" : "campaign-public-list"%>" aria-label="Danh sách chiến dịch">
            <% if (admin) { %><h2>Danh sách chiến dịch</h2><% } %>
            <% if (campaigns.isEmpty()) { %><p class="workspace-muted">Chưa có chiến dịch <%=admin ? "nào" : "đang diễn ra"%>.</p><% } %>
            <% for (Campaign campaign : campaigns) { %>
            <article class="campaign-item">
                <header><span class="campaign-period"><%=date.format(campaign.start())%> – <%=date.format(campaign.end())%></span>
                    <% if (admin) { %><span class="workspace-badge"><%=campaign.status(today)%></span><% } %></header>
                <h2><%=Html.escape(campaign.title())%></h2>
                <p><%=Html.escape(campaign.description())%></p>
                <% if (campaign.voucherCode() != null) { %><div class="campaign-code">Mã ưu đãi <strong><%=Html.escape(campaign.voucherCode())%></strong></div><% } %>
                <% if (!admin && campaign.voucherCode() != null) { %>
                    <% if (session.getAttribute("user") == null) { %><a class="workspace-link" href="<%=base%>/login">Đăng nhập để nhận mã</a><% } else { %>
                    <form method="post" action="<%=base%>/campaigns">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="campaignId" value="<%=campaign.id()%>">
                        <button class="workspace-button" type="submit">Nhận voucher</button>
                    </form>
                    <% } %>
                <% } %>
                <% if (admin) { %><a class="workspace-link" href="<%=base%>/admin-campaigns?id=<%=campaign.id()%>">Chỉnh sửa chiến dịch</a><% } %>
            </article>
            <% } %>
        </section>
    </div>
</main>
<% if (!admin) { %><jsp:include page="/footer.jsp" /><% } %>
</body>
</html>
