package util;

/**
 * Quy định mật khẩu dùng chung cho đăng ký, đổi mật khẩu trong hồ sơ và đặt lại mật khẩu bằng OTP (IS-08).
 */
public final class PasswordPolicy {
    public static final String MESSAGE = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và chữ số.";
    private static final String PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && password.matches(PATTERN);
    }
}
