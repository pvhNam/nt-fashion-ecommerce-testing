package service;

import java.util.List;

import dao.OrderDAO;
import dao.VoucherDAO;
import model.Order;

/**
 * Xử lý đơn MoMo/VNPay chờ thanh toán (ST-16, IS-05):
 * - đơn thất bại hoặc quá hạn chờ thanh toán chuyển sang "thất bại";
 * - voucher mà đơn đã giữ được trả lại ví của khách.
 * Không có tác vụ chạy nền: hạn được kiểm tra mỗi khi khách/admin mở giỏ hàng, thanh toán
 * hoặc xem đơn hàng.
 */
public class PendingPaymentService {
    public static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final OrderDAO orderDao = new OrderDAO();
    private final VoucherDAO voucherDao = new VoucherDAO();

    /** Hủy các đơn chờ thanh toán quá hạn của một khách. */
    public void expireOverdue(int userId) {
        expire(orderDao.getOverduePendingOnlineOrders(userId, PAYMENT_TIMEOUT_MINUTES));
    }

    /** Hủy các đơn chờ thanh toán quá hạn của mọi khách (trang quản trị đơn hàng). */
    public void expireAllOverdue() {
        expire(orderDao.getOverduePendingOnlineOrders(null, PAYMENT_TIMEOUT_MINUTES));
    }

    private void expire(List<Order> overdueOrders) {
        for (Order order : overdueOrders) {
            if (Order.STATUS_PENDING_MOMO.equals(order.getStatus())) {
                markFailed(order, Order.STATUS_PENDING_MOMO, Order.STATUS_MOMO_FAILED);
            } else if (Order.STATUS_PENDING_VNPAY.equals(order.getStatus())) {
                markFailed(order, Order.STATUS_PENDING_VNPAY, Order.STATUS_VNPAY_FAILED);
            }
        }
    }

    /**
     * Chuyển đơn từ trạng thái chờ sang thất bại (chỉ khi đơn vẫn đang chờ) và trả voucher.
     * Trả về true nếu chính lần gọi này đã đổi trạng thái đơn.
     */
    public boolean markFailed(Order order, String pendingStatus, String failedStatus) {
        if (order == null) {
            return false;
        }
        if (!orderDao.updateOrderStatusIfCurrent(order.getId(), pendingStatus, failedStatus)) {
            return false;
        }
        if (order.getVoucherId() > 0) {
            voucherDao.releaseVoucher(order.getUserId(), order.getVoucherId());
        }
        new OrderNotificationService().notifyStatusChanged(orderDao.getOrderById(order.getId()), null);
        return true;
    }
}
