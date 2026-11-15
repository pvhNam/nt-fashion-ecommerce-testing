<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%
  Object codeAttr = request.getAttribute("jakarta.servlet.error.status_code");
  int code = codeAttr instanceof Integer ? (Integer) codeAttr : 500;
  String title;
  String detail;
  if (code == 404) {
    title = "Không tìm thấy trang";
    detail = "Trang bạn tìm không tồn tại hoặc đã bị chuyển đi.";
  } else if (code == 403) {
    title = "Không có quyền truy cập";
    detail = "Tài khoản của bạn không được phép xem trang này.";
  } else if (code >= 400 && code < 500) {
    title = "Yêu cầu không hợp lệ";
    detail = "Yêu cầu gửi lên không đúng. Vui lòng thử lại từ trang chủ.";
  } else {
    title = "Đã có lỗi xảy ra";
    detail = "Hệ thống đang gặp sự cố. Vui lòng thử lại sau ít phút.";
  }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><%= title %> | Fashion Store</title>
  <style>
    body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
           font-family: Arial, sans-serif; background: #f6f6f6; color: #222; }
    .box { max-width: 420px; margin: 16px; padding: 32px 24px; background: #fff; border-radius: 8px;
           box-shadow: 0 2px 10px rgba(0,0,0,.08); text-align: center; }
    .code { font-size: 48px; font-weight: 700; color: #999; margin: 0; }
    h1 { font-size: 22px; margin: 8px 0 12px; }
    p { color: #555; line-height: 1.5; }
    a { display: inline-block; margin-top: 12px; padding: 10px 20px; background: #222; color: #fff;
        border-radius: 4px; text-decoration: none; }
  </style>
</head>
<body>
  <div class="box">
    <p class="code"><%= code %></p>
    <h1><%= title %></h1>
    <p><%= detail %></p>
    <a href="<%= request.getContextPath() %>/home">Về trang chủ</a>
  </div>
</body>
</html>
