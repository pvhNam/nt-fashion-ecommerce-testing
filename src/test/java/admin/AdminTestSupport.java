package admin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;

/** Shared HTTP support for the Admin tests, discovered with ARTEMIS and ADB. */
abstract class AdminTestSupport {
    private final Map<String, AdminClient> clients = new HashMap<>();

    @BeforeAll
    static void requireTestEnvironment() {
        assertEquals("1", System.getenv("ADMIN_TEST_ISOLATED_DB"),
                "Configure Tomcat with a separate QA database and disabled notifications, "
                + "then set ADMIN_TEST_ISOLATED_DB=1. See src/test/README.md.");
        new AdminClient().verifyLoginPage();
    }

    protected Credentials credentials(String role) {
        String defaultUser = switch (role) {
            case "admin" -> "nt_seed_admin";
            case "staff" -> "nt_seed_staff";
            case "customer" -> "nt_seed_standard";
            default -> throw new IllegalArgumentException("Unknown test role.");
        };
        String prefix = "ADMIN_TEST_" + role.toUpperCase(java.util.Locale.ROOT);
        String user = setting(prefix + "_USER", defaultUser);
        String password = setting(prefix + "_PASSWORD", System.getenv("ADMIN_TEST_PASSWORD"));
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set " + prefix + "_PASSWORD or ADMIN_TEST_PASSWORD.");
        }
        return new Credentials(user, password);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    protected AdminClient session(String role) throws Exception {
        AdminClient existing = clients.get(role);
        if (existing != null) {
            return existing;
        }
        AdminClient client = new AdminClient();
        if (!"guest".equals(role)) {
            Credentials account = credentials(role);
            Response response = client.login(account.username(), account.password());
            assertSuccessfulLogin(response, role, account.username());
        }
        clients.put(role, client);
        return client;
    }

    protected static void assertSuccessfulLogin(Response response, String role, String username) {
        String context = "Login failed for role=" + role + ", username=" + username
                + ", HTTP " + response.status() + ". ";
        assertTrue(Set.of(302, 303).contains(response.status()), () -> {
            String rejection = response.document().selectFirst(".alert-error") != null
                    ? "The login form reported an authentication error. " : "";
            return context + rejection + "Expected HTTP 302 or 303. Check the test account and "
                    + "ADMIN_TEST_PASSWORD or ADMIN_TEST_"
                    + role.toUpperCase(java.util.Locale.ROOT)
                    + "_PASSWORD (the per-role password overrides the shared password).";
        });
        assertAll(context, () -> assertRedirect(response, "customer".equals(role) ? "home" : "admin"));
    }

    protected static void assertRedirect(Response response, String route) {
        assertTrue(Set.of(302, 303).contains(response.status()),
                "Expected redirect after request, got HTTP " + response.status());
        String path = URI.create(response.location()).getPath();
        assertNotNull(path, "Redirect target has no path.");
        assertEquals(route, path.substring(path.lastIndexOf('/') + 1),
                "Unexpected redirect destination.");
    }

    protected static void assertDenied(Response response) {
        if (response.status() == 401 || response.status() == 403) {
            return;
        }
        assertRedirect(response, "login");
    }

    @FunctionalInterface
    protected interface CheckedCondition {
        boolean evaluate() throws Exception;
    }

    protected static void waitUntil(CheckedCondition condition, String message) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        do {
            if (condition.evaluate()) {
                return;
            }
            if (System.nanoTime() >= deadline) {
                fail(message);
            }
            Thread.sleep(100);
        } while (true);
    }

    protected record Credentials(String username, String password) {
        @Override
        public String toString() {
            return "Test credentials [redacted]";
        }
    }

    protected record Response(int status, URI uri, HttpHeaders headers, byte[] body) {
        public String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        public String location() {
            return headers.firstValue("Location").orElse("");
        }

        public Document document() {
            return Jsoup.parse(text(), uri.toString());
        }

        @Override
        public String toString() {
            return "HTTP " + status + " " + uri;
        }
    }

    protected static final class AdminClient {
        private final URI base;
        private final HttpClient http;
        private String csrfToken;

        public AdminClient() {
            String configured = System.getenv("ADMIN_TEST_BASE_URL");
            if (configured == null || configured.isBlank()) {
                throw new IllegalStateException("Set ADMIN_TEST_BASE_URL to the local Tomcat context.");
            }
            base = URI.create(configured.endsWith("/") ? configured : configured + "/");
            if (base.getScheme() == null || base.getHost() == null
                    || !Set.of("http", "https").contains(base.getScheme())
                    || !Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(base.getHost())
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
                throw new IllegalStateException("Test URL must be a local loopback URL without credentials.");
            }
            http = HttpClient.newBuilder()
                    .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .proxy(ProxySelector.of(null))
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }

        public Response get(String path) throws Exception {
            return send("GET", path, null, null);
        }

        public Response post(String path, Map<String, String> fields) throws Exception {
            if (csrfToken == null) {
                get("login");
            }
            if (csrfToken == null) {
                throw new IllegalStateException("The login form did not render csrfToken.");
            }
            return send("POST", path, fields, csrfToken);
        }

        public Response postWithoutCsrf(String path, Map<String, String> fields) throws Exception {
            return send("POST", path, fields, null);
        }

        public Response postWithToken(String path, Map<String, String> fields, String token)
                throws Exception {
            return send("POST", path, fields, token);
        }

        public Response login(String username, String password) throws Exception {
            Response login = get("login");
            assertEquals(200, login.status(), "The login page failed to load.");
            return post("login", Map.of("username", username, "password", password));
        }

        private void verifyLoginPage() {
            URI loginUrl = base.resolve("login");
            String guidance = " Check Tomcat's HTTP port and Deployment Application context, "
                    + "then update ADMIN_TEST_BASE_URL.";
            Response response;
            try {
                response = send("GET", "login", null, null, Duration.ofSeconds(5));
            } catch (HttpTimeoutException error) {
                throw new IllegalStateException("Admin test preflight timed out after 5 seconds at "
                        + loginUrl + "." + guidance, error);
            } catch (IOException error) {
                throw new IllegalStateException("Admin test preflight could not connect to "
                        + loginUrl + "." + guidance, error);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Admin test preflight was interrupted at "
                        + loginUrl + ".", error);
            }
            if (response.status() != 200) {
                throw new IllegalStateException("Admin test preflight received HTTP "
                        + response.status() + " at " + loginUrl + "." + guidance);
            }
            Document page = response.document();
            if (page.selectFirst("input[name=username]") == null
                    || page.selectFirst("input[name=password]") == null
                    || page.selectFirst("input[name=csrfToken]") == null
                    || page.selectFirst("input[name=csrfToken]").val().isBlank()) {
                throw new IllegalStateException("Admin test preflight did not find the expected "
                        + "login form and CSRF token at " + loginUrl + "." + guidance);
            }
        }

        private Response send(String method, String path, Map<String, String> fields, String token)
                throws IOException, InterruptedException {
            return send(method, path, fields, token, Duration.ofSeconds(15));
        }

        private Response send(String method, String path, Map<String, String> fields, String token,
                Duration timeout) throws IOException, InterruptedException {
            URI target = base.resolve(path).normalize();
            if (!base.getScheme().equals(target.getScheme())
                    || !base.getAuthority().equals(target.getAuthority())
                    || !target.getPath().startsWith(base.getPath())) {
                throw new IllegalArgumentException("Request would leave the configured test application.");
            }
            HttpRequest.Builder request = HttpRequest.newBuilder(target).timeout(timeout);
            if ("POST".equals(method)) {
                String encoded = fields.entrySet().stream()
                        .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                        .collect(Collectors.joining("&"));
                request.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(encoded, StandardCharsets.UTF_8));
                if (token != null) {
                    request.header("X-CSRF-Token", token);
                }
            } else {
                request.GET();
            }
            HttpResponse<byte[]> raw = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            Response response = new Response(raw.statusCode(), raw.uri(), raw.headers(), raw.body());
            if (raw.headers().firstValue("Content-Type").orElse("").contains("html")) {
                Element input = response.document().selectFirst("input[name=csrfToken]");
                if (input != null && !input.val().isBlank()) {
                    csrfToken = input.val();
                }
            }
            return response;
        }

        private static String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
    }
}
