<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="model.News, util.Html"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chỉnh sửa bài viết | Admin</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/admin/Admin.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/CSS/admin/admin-edit-news.css?v=20261126">
</head>
<body>
<jsp:include page="sidebarAdmin.jsp">
    <jsp:param name="pageName" value="news" />
</jsp:include>

<% News n = (News) request.getAttribute("n"); %>
<main class="main-content">
    <header class="content-header">
        <h1 class="page-title">Chỉnh sửa bài viết</h1>
    </header>

    <form class="news-edit-form" action="${pageContext.request.contextPath}/admin-news" method="post">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="id" value="<%=n.getId()%>">

        <div class="news-edit-fields">
            <div class="news-edit-field">
                <label for="news-title">Tiêu đề</label>
                <input id="news-title" type="text" name="title" value="<%=Html.escape(n.getTitle())%>">
            </div>
            <div class="news-edit-field">
                <label for="news-summary">Mô tả ngắn</label>
                <textarea id="news-summary" name="shortDesc" rows="3"><%=Html.escape(n.getShortDesc())%></textarea>
            </div>
            <div class="news-edit-field">
                <label for="news-content">Nội dung chi tiết</label>
                <textarea id="news-content" name="content" rows="10"><%=Html.escape(n.getContent())%></textarea>
            </div>
            <div class="news-edit-field">
                <label for="news-image">Đường dẫn ảnh đại diện</label>
                <input id="news-image" type="text" name="image" value="<%=Html.escape(n.getImage())%>">
            </div>
        </div>

        <div class="news-edit-actions">
            <a class="news-edit-button news-edit-cancel" href="${pageContext.request.contextPath}/admin-news">Hủy bỏ</a>
            <button class="news-edit-button news-edit-save" type="submit">Lưu thay đổi</button>
        </div>
    </form>
</main>
</body>
</html>
