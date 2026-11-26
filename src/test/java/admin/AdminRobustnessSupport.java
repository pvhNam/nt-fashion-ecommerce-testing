package admin;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/** Database oracles and owned fixtures for the ARTEMIS/ADB-verified Admin flows. */
abstract class AdminRobustnessSupport extends AdminTestSupport {
    @BeforeAll
    static void requireRobustnessDatabase() throws Exception {
        String configured = required("ADMIN_TEST_DB_URL");
        if (!configured.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException("ADMIN_TEST_DB_URL must be an explicit local MySQL JDBC URL.");
        }
        URI target = URI.create(configured.substring("jdbc:".length()));
        String schema = target.getPath() == null ? "" : target.getPath().substring(1);
        if (target.getHost() == null
                || !Set.of("127.0.0.1", "localhost", "[::1]", "::1").contains(target.getHost())
                || target.getUserInfo() != null || target.getFragment() != null
                || !schema.matches("[A-Za-z0-9_]+")
                || !schema.toLowerCase(Locale.ROOT).matches(".*(?:^|_)(?:qa|test|testing|adminqa)(?:_|$).*")) {
            throw new IllegalStateException("Robustness tests require a loopback QA/test schema; "
                    + "the application's default database is not used. See src/test/ROBUSTNESS.md.");
        }
        required("ADMIN_TEST_DB_USER");
        if (System.getenv("ADMIN_TEST_DB_PASSWORD") == null) {
            throw new IllegalStateException("Set ADMIN_TEST_DB_PASSWORD explicitly for the QA database.");
        }
        try (Connection connection = openDatabase(); Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT DATABASE()")) {
            assertTrue(result.next());
            assertEquals(schema, result.getString(1), "Unexpected active QA database.");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + ". See src/test/ROBUSTNESS.md.");
        }
        return value;
    }

    private static Connection openDatabase() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", required("ADMIN_TEST_DB_USER"));
        properties.setProperty("password", System.getenv("ADMIN_TEST_DB_PASSWORD"));
        properties.setProperty("connectTimeout", "5000");
        properties.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(required("ADMIN_TEST_DB_URL"), properties);
    }

    protected Connection database() throws Exception {
        return openDatabase();
    }

    /** Prove that JDBC fixtures and the HTTP application address the same database. */
    @BeforeEach
    void verifyApplicationDatabase() throws Exception {
        String title = marker("QA_DB_PROBE_");
        try {
            int id = insert("INSERT INTO news(title,short_desc,content,image) VALUES(?,?,?,?)",
                    title, title, "QA database identity probe", "img/images.jpg");
            Response response = session("admin").get("admin-news?type=edit&id=" + id);
            assertEquals(200, response.status(), "Cannot verify the application's QA database.");
            var field = response.document().selectFirst("form[action=admin-news] input[name=title]");
            assertNotNull(field, "QA identity probe form is missing; check the running app/build.");
            assertEquals(title, field.val(), "Tomcat and ADMIN_TEST_DB_URL do not use the same database; "
                    + "no test mutation was sent.");
        } finally {
            update("DELETE FROM news WHERE title=? AND short_desc=?", title, title);
            assertTrue(rows("SELECT id FROM news WHERE title=?", title).isEmpty(),
                    "The owned database identity probe was not cleaned.");
        }
    }

    protected List<Map<String, Object>> rows(String sql, Object... parameters) throws Exception {
        try (Connection connection = database(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                var metadata = result.getMetaData();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= metadata.getColumnCount(); column++) {
                        row.put(metadata.getColumnLabel(column).toLowerCase(Locale.ROOT), result.getObject(column));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    protected int update(String sql, Object... parameters) throws Exception {
        try (Connection connection = database(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        }
    }

    protected int insert(String sql, Object... parameters) throws Exception {
        try (Connection connection = database();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, parameters);
            assertEquals(1, statement.executeUpdate(), "Could not create an owned QA fixture.");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next(), "Fixture insert did not return its generated ID.");
                return keys.getInt(1);
            }
        }
    }

    private static void bind(PreparedStatement statement, Object[] parameters) throws Exception {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }

    protected static String marker(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    protected static void assertControlledRejection(Response response, String expectedRoute) {
        String context = "Invalid input must be rejected with a controlled validation response at "
                + expectedRoute + "; got HTTP " + response.status();
        if (Set.of(400, 404, 409, 422).contains(response.status())) {
            return;
        }
        if (Set.of(302, 303).contains(response.status())) {
            assertRedirect(response, expectedRoute);
            URI target = response.uri().resolve(response.location());
            assertEquals(response.uri().getAuthority(), target.getAuthority(), "Redirect left the QA server.");
            String query = URLDecoder.decode(target.getRawQuery() == null ? "" : target.getRawQuery(),
                    StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            boolean validationError = false;
            for (String parameter : query.split("&")) {
                String[] pair = parameter.split("=", 2);
                if (pair.length == 2 && Set.of("msg", "error").contains(pair[0])) {
                    String value = pair[1];
                    assertFalse(value.matches(".*(?:permission|auth|login|session|csrf|token|forbidden|access[-_]denied).*"),
                            context + "; an authorization/session/CSRF error is not input validation.");
                    validationError |= value.startsWith("error") || value.startsWith("invalid")
                            || Set.of("not-found", "not_found", "duplicate", "duplicate-code").contains(value);
                }
            }
            assertTrue(validationError, context + "; redirect must report validation failure, not success.");
            return;
        }
        if (response.status() == 200) {
            boolean error = response.document()
                    .select(".alert-error, .alert-danger, .validation-error, .error-message")
                    .stream().anyMatch(element -> !element.text().isBlank()
                            && !element.hasAttr("hidden")
                            && !element.attr("style").replace(" ", "").contains("display:none"));
            assertTrue(error, context + "; form must display a validation error.");
            return;
        }
        fail(context + ". HTTP 500 and authentication failures are not validation results.");
    }

    protected static void assertHealthyAdmin(AdminClient client) throws Exception {
        Response response = client.get("admin");
        assertEquals(200, response.status(), "Admin must remain usable after invalid input.");
        assertTrue(response.document().text().contains("Tổng quan Dashboard"),
                "The follow-up response is not the authenticated Admin dashboard.");
    }
}
