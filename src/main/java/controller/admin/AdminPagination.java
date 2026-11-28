package controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AdminPagination {
    private static final int PAGE_SIZE = 10;

    private AdminPagination() {
    }

    static int requestedPage(HttpServletRequest request) {
        try {
            return Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    static <T> List<T> page(HttpServletRequest request, List<T> rows, String... filterNames) {
        List<T> results = rows == null ? List.of() : rows;
        int total = results.size();
        int pageCount = Math.max(1, Math.ceilDiv(total, PAGE_SIZE));
        int page = Math.min(requestedPage(request), pageCount);
        int from = (page - 1) * PAGE_SIZE;
        int to = from + Math.min(PAGE_SIZE, total - from);

        Map<String, String> filters = new LinkedHashMap<>();
        for (String name : filterNames) {
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) {
                filters.put(name, value);
            }
        }

        request.setAttribute("adminPage", page);
        request.setAttribute("adminPageSize", PAGE_SIZE);
        request.setAttribute("adminPageCount", pageCount);
        request.setAttribute("adminTotalRows", total);
        request.setAttribute("adminFirstRow", total == 0 ? 0 : from + 1);
        request.setAttribute("adminLastRow", to);
        request.setAttribute("adminPagePath", request.getServletPath());
        request.setAttribute("adminPageFilters", filters);
        return results.subList(from, to);
    }
}
