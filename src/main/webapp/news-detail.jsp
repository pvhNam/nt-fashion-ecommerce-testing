<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="model.News, model.User"%>
<%!
  private String h(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${sessionScope.lang != null ? sessionScope.lang : 'vi'}" />
<fmt:setBundle basename="resources.messages" />

<!DOCTYPE html>
<html lang="${sessionScope.lang}">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<% News n = (News) request.getAttribute("newsObj");
   if (n == null) { response.sendRedirect("news.jsp"); return; } %>
<title><%=h(n.getTitle())%> | Fashion Store</title>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<link rel="stylesheet" href="CSS/style.css" />
<link rel="stylesheet" href="CSS/user/news-detail.css">
</head>
<body class="ym-user-page ym-article-page">
    <jsp:include page="header.jsp"><jsp:param name="page" value="news" /></jsp:include>
    <div class="article-container">
        <div class="article-header">
            <h1 class="article-title"><%=h(n.getTitle())%></h1>
            <div class="article-meta">
                <span><i class="fa-regular fa-calendar"></i> <%=n.getCreatedAt()%></span>
                <span style="margin: 0 10px;">|</span> <span>Fashion Store Team</span>
            </div>
        </div>
        <img src="<%=h(n.getImage())%>" alt="Img" class="article-featured-img" onerror="this.src='img/no-image.png'">
        <div class="article-content">
            <p><strong><%=h(n.getShortDesc())%></strong></p>
            <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
            <% String content = n.getContent();
               // Nội dung tin là chữ thường: mã hóa trước rồi mới đổi xuống dòng thành <br>
               if (content != null) { content = h(content).replace("\n", "<br>"); }
               else { content = "<p>Updating...</p>"; } %>
            <%=content%>
        </div>
        <div class="article-footer">
            <a href="news.jsp" class="btn-back"> <i class="fa-solid fa-arrow-left"></i> <fmt:message key="news.back" /></a>
        </div>
    </div>
        <jsp:include page="footer.jsp" />
</body>
</html>
