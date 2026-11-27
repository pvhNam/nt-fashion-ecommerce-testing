package service;

import dao.InvoiceDAO.Buyer;
import model.Order;
import model.OrderDetail;
import util.EmailUtil;
import util.Html;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class InvoiceEmailService {
    public void send(Order order, Buyer buyer, List<OrderDetail> lines) {
        String reference = "đơn #" + order.getId();
        EmailUtil.sendHtmlAttachment(order.getContactEmail(), "Nam Thành Fashion | Hóa đơn " + reference,
                "Nam Thành gửi bạn hóa đơn " + reference + " trong tệp đính kèm. "
                        + "Bạn cũng có thể đăng nhập, mở chi tiết đơn hàng và chọn Xem hóa đơn.",
                "Hoa-don-" + order.getId() + ".html", attachment(order, buyer, lines));
    }

    String attachment(Order order, Buyer buyer, List<OrderDetail> lines) {
        NumberFormat money = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        StringBuilder html = new StringBuilder("<!doctype html><html lang=\"vi\"><meta charset=\"UTF-8\">"
                + "<title>Hóa đơn Nam Thành</title><style>body{font:15px/1.5 system-ui,sans-serif;max-width:850px;margin:32px auto;padding:0 16px}"
                + "table{width:100%;border-collapse:collapse}th,td{padding:8px;border-bottom:1px solid #ccc;text-align:left}</style>")
                .append("<h1>Hóa đơn bán hàng</h1><p>Nam Thành Fashion · Đơn #").append(order.getId()).append("</p>");
        if (order.getCreatedDateTime() != null) {
            html.append("<p>Ngày đặt ").append(order.getCreatedDateTime().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))).append("</p>");
        }
        html.append("<p>").append(buyer.isCompany() ? "Công ty " : "Cá nhân ").append(Html.escape(buyer.name())).append("</p>")
                .append("<p>Địa chỉ ").append(Html.escape(buyer.address())).append("</p>");
        if (buyer.isCompany()) html.append("<p>Mã số thuế ").append(Html.escape(buyer.taxCode())).append("</p>");
        html.append("<table><thead><tr><th>Mặt hàng</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>");
        double subtotal = 0;
        for (OrderDetail line : lines) {
            double amount = line.getPrice() * line.getQuantity();
            subtotal += amount;
            html.append("<tr><td>").append(Html.escape(line.getProduct().getPdescription())).append(" ")
                    .append(Html.escape(line.getColor())).append(" ").append(Html.escape(line.getSize()))
                    .append("</td><td>").append(line.getQuantity()).append("</td><td>").append(money.format(line.getPrice()))
                    .append("</td><td>").append(money.format(amount)).append("</td></tr>");
        }
        html.append("</tbody></table>");
        long adjustment = Math.round(order.getTotalMoney()) - Math.round(subtotal);
        if (adjustment != 0) html.append("<p>Điều chỉnh đơn hàng ").append(money.format(adjustment)).append("</p>");
        return html.append("<p><strong>Tổng tiền đơn hàng ").append(money.format(order.getTotalMoney()))
                .append("</strong></p><p>Bản in từ đơn hàng, dùng để đối chiếu thông tin mua hàng.</p></html>").toString();
    }
}
