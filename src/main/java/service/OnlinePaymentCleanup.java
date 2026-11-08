package service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import dao.CartDAO;
import dao.OrderDAO;
import dao.VoucherDAO;
import model.CartItem;
import model.Order;
import model.OrderDetail;
import model.Product;

/** Dọn giỏ hàng và voucher sau khi MoMo/VNPay báo thanh toán thành công (ST-16). */
public final class OnlinePaymentCleanup {
    private static final Logger LOGGER = Logger.getLogger(OnlinePaymentCleanup.class.getName());

    private OnlinePaymentCleanup() {
    }

    /**
     * @param fallbackVoucherId voucher ghi trong dữ liệu gửi cổng thanh toán (cho đơn tạo trước khi có cột voucher_id)
     * @param wasMarkedFailed   đơn đã bị đánh thất bại/hết hạn (voucher đã được trả về ví) trước khi tiền về
     */
    public static void afterSuccess(OrderDAO orderDao, Order order, int fallbackVoucherId, boolean wasMarkedFailed) {
        // Chỉ bỏ khỏi giỏ các món thuộc đơn này, không xóa cả giỏ
        List<CartItem> purchasedItems = new ArrayList<>();
        for (OrderDetail detail : orderDao.getDetails(order.getId())) {
            Product product = detail.getProduct();
            if (product == null) {
                continue;
            }
            product.setPid(detail.getProductId());
            purchasedItems.add(new CartItem(product, detail.getQuantity(), detail.getColor(), detail.getSize()));
        }
        new CartDAO().removePurchasedItems(order.getUserId(), purchasedItems);

        int voucherId = order.getVoucherId() > 0 ? order.getVoucherId() : fallbackVoucherId;
        if (voucherId <= 0) {
            return;
        }
        // Voucher thường đã được giữ lúc tạo đơn; câu lệnh chỉ đổi khi voucher đã được trả về ví
        boolean marked = new VoucherDAO().markVoucherUsed(order.getUserId(), voucherId);
        if (!marked && wasMarkedFailed) {
            LOGGER.warning("Order " + order.getId() + " was paid after expiry but voucher " + voucherId
                    + " is no longer available in the wallet of user " + order.getUserId());
        }
    }
}
