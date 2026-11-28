package dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnect {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/ltweb?useSSL=false&allowPublicKeyRetrieval=true";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                    setting("db.url", "DB_URL", DEFAULT_URL),
                    required("db.user", "DB_USER"),
                    required("db.password", "DB_PASSWORD"));
            System.out.println("Kết nối thành công!");
        } catch (Exception e) {
            System.out.println("Kết nối thất bại!");
            e.printStackTrace();
        }
        return conn;
    }
    
    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null) value = System.getenv(environment);
        return value == null ? fallback : value;
    }

    private static String required(String property, String environment) {
        String value = setting(property, environment, null);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Chưa cấu hình " + environment + " (biến môi trường hoặc -D" + property + ").");
        }
        return value;
    }

    // test
    public static void main(String[] args) {
        getConnection();
    }
}
