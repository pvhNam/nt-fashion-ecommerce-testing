package util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Băm mật khẩu PBKDF2-SHA256 có muối; vẫn nhận mã MD5 cũ để nâng cấp khi đăng nhập. */
public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS, HASH_BITS);
        Base64.Encoder base64 = Base64.getEncoder().withoutPadding();
        return PREFIX + "$" + ITERATIONS + "$" + base64.encodeToString(salt) + "$" + base64.encodeToString(hash);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        if (isLegacyMd5(stored)) {
            return MessageDigest.isEqual(MD5.getMd5(password).getBytes(StandardCharsets.US_ASCII),
                    stored.toLowerCase().getBytes(StandardCharsets.US_ASCII));
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (iterations <= 0 || salt.length == 0 || expected.length == 0) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt, iterations, expected.length * 8));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean needsRehash(String stored) {
        return stored == null || !stored.startsWith(PREFIX + "$" + ITERATIONS + "$");
    }

    private static boolean isLegacyMd5(String stored) {
        return stored.matches("[0-9a-fA-F]{32}");
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
