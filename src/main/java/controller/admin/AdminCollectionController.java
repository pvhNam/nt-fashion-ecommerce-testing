package controller.admin;

import dao.CollectionDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.CollectionTheme;
import model.Product;

@WebServlet("/admin-collections")
public class AdminCollectionController extends HttpServlet {
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
    private static final String VIEW = "/WEB-INF/views/admin-collections.jsp";
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String themeSlug = queryValue(request, "theme");
        if (themeSlug.isEmpty() && "create".equals(request.getParameter("view"))) {
            request.setAttribute("createView", true);
            request.getRequestDispatcher(VIEW).forward(request, response);
            return;
        }
        String search = queryValue(request, "search");
        request.setAttribute("searchKeyword", search);

        CollectionDAO dao = new CollectionDAO();
        try {
            List<CollectionTheme> themes = dao.getThemes();
            CollectionTheme theme = CollectionTheme.findBySlug(themes, themeSlug);
            if (!themeSlug.isEmpty() && theme == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean addView = theme != null && "add".equals(request.getParameter("view"));
            request.setAttribute("selectedTheme", theme);
            request.setAttribute("addView", addView);
            if (theme == null) {
                request.setAttribute("hiddenCollectionSlugs", dao.getHiddenSlugs());
                loadCollectionList(request, search, themes, dao.getCounts());
            } else {
                List<Product> products = addView ? dao.getAvailableProducts(theme) : dao.getProducts(theme);
                String keyword = normalizeTitle(search);
                if (!keyword.isEmpty()) {
                    products = products.stream()
                            .filter(product -> normalizeTitle(product.getPdescription()).contains(keyword))
                            .toList();
                }
                request.setAttribute("products", AdminPagination.page(request, products, "theme", "view", "search"));
            }
        } catch (SQLException error) {
            getServletContext().log("Unable to load collection management", error);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("collectionUnavailable", true);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void loadCollectionList(HttpServletRequest request, String search, List<CollectionTheme> themes,
            Map<String, Integer> counts) {
        String sort = queryValue(request, "sort");
        Comparator<CollectionTheme> byTitle = Comparator.comparing(CollectionTheme::getNameVi, Collator.getInstance(VIETNAMESE));
        Comparator<CollectionTheme> order = switch (sort) {
            case "title-desc" -> byTitle.reversed();
            case "products-desc" -> Comparator.<CollectionTheme>comparingInt(theme -> counts.getOrDefault(theme.getSlug(), 0))
                    .reversed().thenComparing(byTitle);
            default -> byTitle;
        };
        String keyword = normalizeTitle(search);
        List<CollectionTheme> rows = themes.stream()
                .filter(theme -> normalizeTitle(theme.getNameVi()).contains(keyword)
                        || normalizeTitle(theme.getNameEn()).contains(keyword))
                .sorted(order)
                .toList();
        request.setAttribute("collectionRows", rows);
        request.setAttribute("collectionCounts", counts);
        request.setAttribute("sortOrder", sort);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        if ("create".equals(action)) {
            createCollection(request, response);
            return;
        }
        if ("show".equals(action) || "hide".equals(action)) {
            updateVisibility(request, response, "show".equals(action));
            return;
        }
        if ("update-description".equals(action)) {
            updateDescription(request, response);
            return;
        }
        String themeSlug = queryValue(request, "theme");
        boolean add = "add".equals(action);
        if (themeSlug.isEmpty() || (!add && !"remove".equals(action))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String message = add ? "added" : "removed";
        try {
            int productId = Integer.parseInt(request.getParameter("pid"));
            if (productId <= 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            CollectionDAO dao = new CollectionDAO();
            CollectionTheme theme = CollectionTheme.findBySlug(dao.getThemes(), themeSlug);
            if (theme == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            themeSlug = theme.getSlug();
            if (add) {
                dao.addProduct(theme, productId);
            } else {
                dao.removeProduct(theme, productId);
            }
        } catch (IllegalArgumentException error) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (SQLException error) {
            getServletContext().log("Unable to save product collections", error);
            message = "error";
        }
        String redirect = request.getContextPath() + "/admin-collections?theme="
                + URLEncoder.encode(themeSlug, StandardCharsets.UTF_8)
                + (add ? "&view=add" : "") + "&msg=" + message
                + "&search=" + URLEncoder.encode(queryValue(request, "search"), StandardCharsets.UTF_8)
                + "&page=" + AdminPagination.requestedPage(request);
        response.sendRedirect(redirect);
    }

    private void updateVisibility(HttpServletRequest request, HttpServletResponse response, boolean visible)
            throws IOException {
        String status;
        try {
            CollectionDAO dao = new CollectionDAO();
            CollectionTheme theme = CollectionTheme.findBySlug(dao.getThemes(), queryValue(request, "theme"));
            if (theme == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            dao.setVisible(theme.getSlug(), visible);
            status = visible ? "shown" : "hidden";
        } catch (SQLException error) {
            getServletContext().log("Unable to update collection visibility", error);
            status = "error";
        }
        response.sendRedirect(request.getContextPath() + "/admin-collections?visibility=" + status
                + "&search=" + URLEncoder.encode(queryValue(request, "search"), StandardCharsets.UTF_8)
                + "&sort=" + URLEncoder.encode(queryValue(request, "sort"), StandardCharsets.UTF_8));
    }

    private void createCollection(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String title = queryValue(request, "title");
        String description = descriptionValue(request);
        String normalized = normalizeTitle(title);
        boolean duplicatesBuiltIn = CollectionTheme.builtIn().stream().anyMatch(theme ->
                normalizeTitle(theme.getNameVi()).equals(normalized)
                || normalizeTitle(theme.getNameEn()).equals(normalized));
        String error;
        int status;
        if (normalized.isBlank() || title.length() > 120) {
            error = "Nhập tiêu đề từ 1 đến 120 ký tự.";
            status = HttpServletResponse.SC_BAD_REQUEST;
        } else if (description.length() > MAX_DESCRIPTION_LENGTH) {
            error = "Nội dung bộ sưu tập tối đa 2.000 ký tự.";
            status = HttpServletResponse.SC_BAD_REQUEST;
        } else if (duplicatesBuiltIn) {
            error = "Tiêu đề này đã tồn tại.";
            status = HttpServletResponse.SC_CONFLICT;
        } else {
            try {
                CollectionTheme theme = new CollectionDAO().createTheme(title, description);
                response.sendRedirect(request.getContextPath() + "/admin-collections?theme=" + theme.getSlug() + "&msg=created");
                return;
            } catch (SQLIntegrityConstraintViolationException duplicate) {
                error = "Tiêu đề này đã tồn tại.";
                status = HttpServletResponse.SC_CONFLICT;
            } catch (SQLException failure) {
                getServletContext().log("Unable to create collection", failure);
                error = "Không tạo được bộ sưu tập. Vui lòng thử lại.";
                status = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            }
        }
        response.setStatus(status);
        request.setAttribute("createView", true);
        request.setAttribute("collectionTitle", title);
        request.setAttribute("collectionDescription", description);
        request.setAttribute("createError", error);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void updateDescription(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String slug = queryValue(request, "theme");
        String description = descriptionValue(request);
        String result;
        try {
            CollectionDAO dao = new CollectionDAO();
            CollectionTheme theme = CollectionTheme.findBySlug(dao.getThemes(), slug);
            if (theme == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            slug = theme.getSlug();
            if (description.length() > MAX_DESCRIPTION_LENGTH) {
                result = "too_long";
            } else {
                dao.saveDescription(slug, description);
                result = "saved";
            }
        } catch (SQLException error) {
            getServletContext().log("Unable to save collection description", error);
            result = "error";
        }
        response.sendRedirect(request.getContextPath() + "/admin-collections?theme="
                + URLEncoder.encode(slug, StandardCharsets.UTF_8) + "&descriptionStatus=" + result);
    }

    private static String descriptionValue(HttpServletRequest request) {
        return queryValue(request, "description").replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String queryValue(HttpServletRequest request, String parameter) {
        String value = request.getParameter(parameter);
        return value == null ? "" : value.strip();
    }

    private static String normalizeTitle(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(VIETNAMESE), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
