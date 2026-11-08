package util;

/**
 * Kiểm tra dữ liệu khách nhập ở đăng ký và sổ địa chỉ (ST-14, IS-12, IS-13).
 */
public final class InputValidator {
    private static final String EMAIL_PATTERN = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";

    private InputValidator() {
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 100 && email.matches(EMAIL_PATTERN);
    }

    /**
     * Số điện thoại Việt Nam: 10 chữ số, bắt đầu bằng 0. Nhận cả dạng +84/84 và dấu cách, chấm, gạch
     * rồi đổi về dạng 0xxxxxxxxx. Trả về null nếu không hợp lệ.
     */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.trim().replaceAll("[\\s.-]", "");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        return digits.matches("0\\d{9}") ? digits : null;
    }

    /**
     * Trả về thông báo lỗi nếu địa chỉ giao hàng thiếu thông tin, null nếu hợp lệ.
     * phone là số đã qua normalizePhone.
     */
    public static String addressError(String receiverName, String phone, String specificAddress,
            String city, String district, String ward) {
        if (receiverName.isEmpty() || receiverName.length() > 100) {
            return "Vui lòng nhập tên người nhận (tối đa 100 ký tự).";
        }
        if (phone == null) {
            return "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.";
        }
        if (city.isEmpty() || district.isEmpty() || ward.isEmpty()) {
            return "Vui lòng chọn đầy đủ tỉnh/thành phố, quận/huyện và phường/xã.";
        }
        if (specificAddress.isEmpty() || specificAddress.length() > 255) {
            return "Vui lòng nhập địa chỉ cụ thể (tối đa 255 ký tự).";
        }
        return null;
    }
}
