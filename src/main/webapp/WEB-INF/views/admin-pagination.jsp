<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${adminPageCount > 1}">
    <c:set var="firstNearby" value="${adminPage - 2 > 1 ? adminPage - 2 : 1}" />
    <c:set var="lastNearby" value="${adminPage + 2 < adminPageCount ? adminPage + 2 : adminPageCount}" />
    <nav class="admin-pagination" aria-label="Phân trang danh sách">
        <span class="admin-pagination-summary">Hiển thị ${adminFirstRow}–${adminLastRow} / ${adminTotalRows}</span>
        <ul>
            <c:if test="${adminPage > 1}">
                <c:url var="previousPageUrl" value="${adminPagePath}">
                    <c:forEach var="filter" items="${adminPageFilters}"><c:param name="${filter.key}" value="${filter.value}" /></c:forEach>
                    <c:param name="page" value="${adminPage - 1}" />
                </c:url>
                <li><a href="<c:out value='${previousPageUrl}' />" rel="prev">Trước</a></li>
            </c:if>
            <c:if test="${firstNearby > 1}">
                <c:url var="firstPageUrl" value="${adminPagePath}">
                    <c:forEach var="filter" items="${adminPageFilters}"><c:param name="${filter.key}" value="${filter.value}" /></c:forEach>
                    <c:param name="page" value="1" />
                </c:url>
                <li><a href="<c:out value='${firstPageUrl}' />" aria-label="Trang 1">1</a></li>
                <c:if test="${firstNearby > 2}"><li aria-hidden="true">…</li></c:if>
            </c:if>
            <c:forEach var="pageNumber" begin="${firstNearby}" end="${lastNearby}">
                <c:choose>
                    <c:when test="${pageNumber == adminPage}">
                        <li><span aria-current="page" aria-label="Trang ${pageNumber}">${pageNumber}</span></li>
                    </c:when>
                    <c:otherwise>
                        <c:url var="pageUrl" value="${adminPagePath}">
                            <c:forEach var="filter" items="${adminPageFilters}"><c:param name="${filter.key}" value="${filter.value}" /></c:forEach>
                            <c:param name="page" value="${pageNumber}" />
                        </c:url>
                        <li><a href="<c:out value='${pageUrl}' />" aria-label="Trang ${pageNumber}">${pageNumber}</a></li>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
            <c:if test="${lastNearby < adminPageCount}">
                <c:if test="${lastNearby < adminPageCount - 1}"><li aria-hidden="true">…</li></c:if>
                <c:url var="lastPageUrl" value="${adminPagePath}">
                    <c:forEach var="filter" items="${adminPageFilters}"><c:param name="${filter.key}" value="${filter.value}" /></c:forEach>
                    <c:param name="page" value="${adminPageCount}" />
                </c:url>
                <li><a href="<c:out value='${lastPageUrl}' />" aria-label="Trang ${adminPageCount}">${adminPageCount}</a></li>
            </c:if>
            <c:if test="${adminPage < adminPageCount}">
                <c:url var="nextPageUrl" value="${adminPagePath}">
                    <c:forEach var="filter" items="${adminPageFilters}"><c:param name="${filter.key}" value="${filter.value}" /></c:forEach>
                    <c:param name="page" value="${adminPage + 1}" />
                </c:url>
                <li><a href="<c:out value='${nextPageUrl}' />" rel="next">Sau</a></li>
            </c:if>
        </ul>
    </nav>
</c:if>
