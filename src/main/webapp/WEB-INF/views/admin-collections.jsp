<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:url var="collectionsUrl" value="/admin-collections" />
<c:url var="createUrl" value="/admin-collections"><c:param name="view" value="create" /></c:url>
<c:if test="${not empty selectedTheme}">
    <c:url var="selectedUrl" value="/admin-collections">
        <c:param name="theme" value="${selectedTheme.slug}" />
    </c:url>
    <c:url var="addUrl" value="/admin-collections">
        <c:param name="theme" value="${selectedTheme.slug}" />
        <c:param name="view" value="add" />
    </c:url>
</c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bộ sưu tập | NT Fashion Admin</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="CSS/admin/Admin.css">
    <link rel="stylesheet" href="CSS/admin/admin-collections.css?v=20261128.8">
</head>
<body>
<jsp:include page="/sidebarAdmin.jsp"><jsp:param name="pageName" value="collections" /></jsp:include>
<main class="main-content collections-page">
    <c:if test="${createView or not empty selectedTheme}">
        <a class="collections-back" href="<c:out value='${addView ? selectedUrl : collectionsUrl}' />">
            <i class="fa-solid fa-arrow-left" aria-hidden="true"></i>
            <c:out value="${addView ? selectedTheme.nameVi : 'Bộ sưu tập'}" />
        </a>
    </c:if>
    <header class="collections-header">
        <h1>
            <c:choose>
                <c:when test="${createView}">Thêm tiêu đề</c:when>
                <c:when test="${addView}">Thêm sản phẩm vào <c:out value="${selectedTheme.nameVi}" /></c:when>
                <c:otherwise><c:out value="${empty selectedTheme ? 'Bộ sưu tập' : selectedTheme.nameVi}" /></c:otherwise>
            </c:choose>
        </h1>
        <c:if test="${empty selectedTheme and not createView and not collectionUnavailable}">
            <a class="collections-button collections-primary" href="<c:out value='${createUrl}' />">Thêm tiêu đề</a>
        </c:if>
        <c:if test="${not empty selectedTheme and not addView and not collectionUnavailable}">
            <a class="collections-button collections-primary" href="<c:out value='${addUrl}' />">Thêm sản phẩm</a>
        </c:if>
    </header>

    <c:choose>
        <c:when test="${collectionUnavailable}">
            <p class="collections-message" role="alert">Không tải được dữ liệu bộ sưu tập. Vui lòng thử lại.</p>
        </c:when>
        <c:when test="${createView}">
            <form class="collections-card collections-create-form" action="<c:out value='${collectionsUrl}' />" method="post">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                <input type="hidden" name="action" value="create">
                <label for="collection-title">Tiêu đề bộ sưu tập</label>
                <input id="collection-title" type="text" name="title" maxlength="120" required
                       value="<c:out value='${collectionTitle}' />">
                <label for="collection-description">Nội dung bộ sưu tập</label>
                <textarea id="collection-description" name="description" rows="5" maxlength="2000"><c:out value="${collectionDescription}" /></textarea>
                <c:if test="${not empty createError}">
                    <p class="collections-message" role="alert"><c:out value="${createError}" /></p>
                </c:if>
                <div class="collections-create-actions">
                    <a class="collections-button" href="<c:out value='${collectionsUrl}' />">Hủy</a>
                    <button class="collections-button collections-primary" type="submit">Tạo bộ sưu tập</button>
                </div>
            </form>
        </c:when>
        <c:when test="${empty selectedTheme}">
            <c:if test="${param.visibility eq 'shown'}"><p class="collections-message" role="status">Đã hiển thị bộ sưu tập trên cửa hàng.</p></c:if>
            <c:if test="${param.visibility eq 'hidden'}"><p class="collections-message" role="status">Đã ẩn bộ sưu tập khỏi cửa hàng.</p></c:if>
            <c:if test="${param.visibility eq 'error'}"><p class="collections-message" role="alert">Không cập nhật được trạng thái. Vui lòng thử lại.</p></c:if>
            <section class="collections-card" aria-label="Danh sách bộ sưu tập">
                <form class="collections-toolbar" action="<c:out value='${collectionsUrl}' />" method="get" role="search">
                    <a class="collections-all" href="<c:out value='${collectionsUrl}' />">Tất cả</a>
                    <label class="collections-search">
                        <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
                        <input type="search" name="search" aria-label="Tìm bộ sưu tập"
                               placeholder="Tìm kiếm bộ sưu tập" value="<c:out value='${searchKeyword}' />">
                    </label>
                    <select name="sort" aria-label="Sắp xếp bộ sưu tập">
                        <option value="title-asc" ${sortOrder ne 'title-desc' and sortOrder ne 'products-desc' ? 'selected' : ''}>Tiêu đề A–Z</option>
                        <option value="title-desc" ${sortOrder eq 'title-desc' ? 'selected' : ''}>Tiêu đề Z–A</option>
                        <option value="products-desc" ${sortOrder eq 'products-desc' ? 'selected' : ''}>Nhiều sản phẩm nhất</option>
                    </select>
                    <button class="collections-button" type="submit">Áp dụng</button>
                </form>
                <div class="collections-table-scroll" tabindex="0" role="region" aria-label="Bảng bộ sưu tập">
                    <table class="collections-table">
                        <thead><tr>
                            <th scope="col">Tiêu đề</th>
                            <th class="collections-count" scope="col">Sản phẩm</th>
                            <th scope="col">Điều kiện</th>
                            <th scope="col">Trạng thái</th>
                        </tr></thead>
                        <tbody>
                        <c:forEach var="theme" items="${collectionRows}">
                            <c:set var="collectionHidden" value="${hiddenCollectionSlugs.contains(theme.slug)}" />
                            <c:url var="detailUrl" value="/admin-collections"><c:param name="theme" value="${theme.slug}" /></c:url>
                            <tr data-collection="${theme.slug}">
                                <th scope="row">
                                    <a class="collections-title-link" href="<c:out value='${detailUrl}' />">
                                        <c:out value="${theme.nameVi}" />
                                    </a>
                                </th>
                                <td class="collections-count"><c:out value="${collectionCounts[theme.slug]}" default="0" /></td>
                                <td class="collections-condition">Gán thủ công</td>
                                <td class="collections-status">
                                    <span class="collections-status-label ${collectionHidden ? '' : 'is-visible'}">
                                        ${collectionHidden ? 'Đang ẩn' : 'Đang hiển thị'}
                                    </span>
                                    <form action="<c:out value='${collectionsUrl}' />" method="post">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                        <input type="hidden" name="theme" value="${theme.slug}">
                                        <input type="hidden" name="search" value="<c:out value='${searchKeyword}' />">
                                        <input type="hidden" name="sort" value="<c:out value='${sortOrder}' />">
                                        <button class="collections-button" type="submit" name="action"
                                                value="${collectionHidden ? 'show' : 'hide'}"
                                                aria-label="${collectionHidden ? 'Hiển thị' : 'Ẩn'} <c:out value='${theme.nameVi}' />">
                                            ${collectionHidden ? 'Hiển thị' : 'Ẩn'}
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty collectionRows}">
                            <tr><td class="collections-empty" colspan="4">Không tìm thấy bộ sưu tập phù hợp.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </c:when>
        <c:otherwise>
            <c:if test="${param.msg eq 'created'}"><p class="collections-message" role="status">Đã tạo bộ sưu tập.</p></c:if>
            <c:if test="${param.msg eq 'added'}"><p class="collections-message" role="status">Đã thêm sản phẩm vào bộ sưu tập.</p></c:if>
            <c:if test="${param.msg eq 'removed'}"><p class="collections-message" role="status">Đã gỡ sản phẩm khỏi bộ sưu tập.</p></c:if>
            <c:if test="${param.msg eq 'error'}"><p class="collections-message" role="alert">Không lưu được thay đổi. Vui lòng thử lại.</p></c:if>
            <c:if test="${not addView}">
                <form class="collections-card collections-create-form collections-description-form"
                      action="<c:out value='${collectionsUrl}' />" method="post">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                    <input type="hidden" name="theme" value="${selectedTheme.slug}">
                    <input type="hidden" name="action" value="update-description">
                    <label for="collection-description">Nội dung bộ sưu tập</label>
                    <textarea id="collection-description" name="description" rows="5" maxlength="2000"><c:out value="${selectedTheme.descriptionVi}" /></textarea>
                    <c:if test="${param.descriptionStatus eq 'saved'}"><p class="collections-message" role="status">Đã lưu nội dung bộ sưu tập.</p></c:if>
                    <c:if test="${param.descriptionStatus eq 'too_long'}"><p class="collections-message" role="alert">Nội dung bộ sưu tập tối đa 2.000 ký tự.</p></c:if>
                    <c:if test="${param.descriptionStatus eq 'error'}"><p class="collections-message" role="alert">Không lưu được nội dung. Vui lòng thử lại.</p></c:if>
                    <div class="collections-create-actions">
                        <button class="collections-button" type="submit">Lưu nội dung</button>
                    </div>
                </form>
            </c:if>
            <section class="collections-card" aria-label="Sản phẩm">
                <form class="collections-toolbar" action="<c:out value='${collectionsUrl}' />" method="get" role="search">
                    <input type="hidden" name="theme" value="${selectedTheme.slug}">
                    <c:if test="${addView}"><input type="hidden" name="view" value="add"></c:if>
                    <label class="collections-search">
                        <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
                        <input type="search" name="search" aria-label="Tìm sản phẩm" placeholder="Tìm kiếm sản phẩm"
                               value="<c:out value='${searchKeyword}' />">
                    </label>
                    <button class="collections-button" type="submit">Tìm</button>
                </form>
                <div class="collections-table-scroll" tabindex="0" role="region" aria-label="Bảng sản phẩm">
                    <table class="collections-table">
                        <thead><tr><th scope="col">Sản phẩm</th><th class="collections-row-action" scope="col">Thao tác</th></tr></thead>
                        <tbody>
                        <c:forEach var="product" items="${products}">
                            <tr id="product-${product.pid}" data-product="${product.pid}">
                                <th scope="row"><c:out value="${product.pdescription}" /></th>
                                <td class="collections-row-action">
                                    <form action="<c:out value='${collectionsUrl}' />" method="post">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                        <input type="hidden" name="pid" value="${product.pid}">
                                        <input type="hidden" name="theme" value="${selectedTheme.slug}">
                                        <input type="hidden" name="search" value="<c:out value='${searchKeyword}' />">
                                        <input type="hidden" name="page" value="${adminPage}">
                                        <button class="collections-button" type="submit" name="action" value="${addView ? 'add' : 'remove'}">
                                            ${addView ? 'Thêm' : 'Gỡ khỏi bộ'}
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty products}">
                            <tr><td class="collections-empty" colspan="2">
                                ${not empty searchKeyword ? 'Không tìm thấy sản phẩm phù hợp.' : (addView ? 'Không còn sản phẩm để thêm vào bộ này.' : 'Bộ sưu tập chưa có sản phẩm.')}
                            </td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <jsp:include page="/WEB-INF/views/admin-pagination.jsp" />
            </section>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
