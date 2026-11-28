<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.ContactDAO, java.util.List, java.time.format.DateTimeFormatter, model.User, util.Html" %>
<%
    boolean admin = Boolean.TRUE.equals(request.getAttribute("adminContact"));
    User contactUser = (User) request.getAttribute("contactUser");
    List<ContactDAO.Thread> threads = (List<ContactDAO.Thread>) request.getAttribute("threads");
    List<ContactDAO.Message> messages = (List<ContactDAO.Message>) request.getAttribute("messages");
    ContactDAO.Thread thread = (ContactDAO.Thread) request.getAttribute("thread");
    String base = request.getContextPath();
    String route = base + (admin ? "/admin-contact" : "/contact");
    DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
%>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%=admin ? "Hộp thư liên hệ" : "Liên hệ"%> | Nam Thành</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <% if (!admin) { %><link rel="stylesheet" href="<%=base%>/CSS/style.css"><% } %>
    <link rel="stylesheet" href="<%=base%>/CSS/shared/customer-tools.css?v=20261126">
    <% if (admin) { %><link rel="stylesheet" href="<%=base%>/CSS/admin/Admin.css"><% } %>
</head>
<body class="<%=admin ? "workspace-admin" : "workspace-public"%>">
<% if (admin) { %>
<jsp:include page="/sidebarAdmin.jsp"><jsp:param name="pageName" value="contact" /></jsp:include>
<% } else { %><jsp:include page="/header.jsp"><jsp:param name="page" value="contact" /></jsp:include><% } %>
<main class="<%=admin ? "main-content" : "workspace-main"%>">
    <header class="workspace-heading">
        <h1><%=admin ? "Hộp thư liên hệ" : "Liên hệ Nam Thành"%></h1>
        <% if (!admin) { %><p><a href="tel:+84981774313">0981 774 313</a> · <a href="mailto:phamvanhoain@gmail.com">phamvanhoain@gmail.com</a></p><% } %>
    </header>
    <% if (contactUser == null) { %>
    <section class="workspace-card">
        <h2>Gửi tin nhắn cho cửa hàng</h2>
        <p>Đăng nhập để gửi câu hỏi và xem phản hồi trong hộp thư của bạn.</p>
        <a class="workspace-button" href="<%=base%>/login">Đăng nhập</a>
    </section>
    <% } else { %>
    <div class="workspace-columns">
        <aside class="workspace-card">
            <h2>Cuộc trao đổi</h2>
            <% if (!admin && thread != null) { %><a class="workspace-link" href="<%=route%>">Tạo cuộc trao đổi</a><% } %>
            <% if (threads == null || threads.isEmpty()) { %><p class="workspace-muted">Chưa có cuộc trao đổi nào.</p><% } else { %>
            <ul class="workspace-list">
                <% for (ContactDAO.Thread item : threads) { %>
                <li><a href="<%=route%>?id=<%=item.id()%>"<%=thread != null && item.id() == thread.id() ? " aria-current=\"page\"" : ""%>>
                    <strong><%=Html.escape(item.subject())%></strong>
                    <span class="workspace-badge"><%=item.answered() ? "Đã phản hồi" : "Chờ cửa hàng phản hồi"%></span>
                    <small><%=admin ? Html.escape(item.customer()) + " · " : ""%><%=timestamp.format(item.updatedAt())%></small>
                </a></li>
                <% } %>
            </ul>
            <% } %>
        </aside>
        <section class="workspace-card">
            <% if (thread == null && admin) { %><p>Chọn một cuộc trao đổi để xem và trả lời.</p><% } else { %>
            <h2><%=thread == null ? "Gửi tin nhắn mới" : Html.escape(thread.subject())%></h2>
            <% if ("1".equals(request.getParameter("sent"))) { %><p class="workspace-notice" role="status">Đã gửi tin nhắn.</p><% } %>
            <% if (messages != null) { %>
            <ol class="contact-messages">
                <% for (ContactDAO.Message message : messages) { %>
                <li class="<%=message.fromAdmin() ? "contact-store" : ""%>">
                    <header><strong><%=message.fromAdmin() ? "Nam Thành" : admin ? Html.escape(thread.customer()) : "Bạn"%></strong>
                        <time datetime="<%=message.createdAt()%>"><%=timestamp.format(message.createdAt())%></time></header>
                    <p><%=Html.escape(message.body())%></p>
                </li>
                <% } %>
            </ol>
            <% } %>
            <form class="workspace-form" action="<%=route%>" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <% if (thread == null) { %>
                <label>Chủ đề<input name="subject" maxlength="160" required></label>
                <% } else { %><input type="hidden" name="id" value="<%=thread.id()%>"><% } %>
                <label><%=thread == null ? "Nội dung" : "Trả lời"%><textarea name="body" rows="5" maxlength="2000" required></textarea></label>
                <button class="workspace-button" type="submit">Gửi tin nhắn</button>
            </form>
            <% } %>
        </section>
    </div>
    <% } %>
</main>
<% if (!admin) { %><jsp:include page="/footer.jsp" /><% } %>
</body>
</html>
