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

    /** Link ảnh chỉ nhận http(s) hoặc đường dẫn trong web, không có dấu cách hay ký tự HTML. */
    public static boolean isSafeImageUrl(String url) {
        if (url == null) {
            return false;
        }
        String u = url.trim();
        if (u.isEmpty() || u.length() > 500 || u.matches("(?s).*[\\s\"'<>`\\\\].*") || u.startsWith("//")) {
            return false;
        }
        String lower = u.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || !lower.matches("^[a-z][a-z0-9+.-]*:.*");
    }

    /**
     * Trả về khóa thông báo lỗi (messages_vi/en) nếu địa chỉ thiếu thông tin, null nếu hợp lệ.
     * phone là số đã qua normalizePhone.
     */
    public static String addressError(String receiverName, String phone, String specificAddress,
            String city, String district, String ward) {
        if (receiverName.isEmpty() || receiverName.length() > 100) {
            return "address.err.name";
        }
        if (phone == null) {
            return "address.err.phone";
        }
        if (city.isEmpty() || district.isEmpty() || ward.isEmpty()) {
            return "address.err.area";
        }
        if (specificAddress.isEmpty() || specificAddress.length() > 255) {
            return "address.err.detail";
        }
        return null;
    }
}
