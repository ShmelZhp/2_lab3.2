<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${currentPath}"/></title>
    <style>
        body { font-family: system-ui, "Segoe UI", Arial, sans-serif; max-width: 960px;
               margin: 24px auto; padding: 0 16px; color: #222; }
        .time { color: #777; font-size: 14px; }
        h1 { font-size: 22px; word-break: break-all; margin: 8px 0 12px;
             padding-bottom: 12px; border-bottom: 1px solid #ddd; }
        .up { display: inline-block; margin: 4px 0 14px; }
        table { width: 100%; border-collapse: collapse; }
        th { text-align: left; padding: 8px; border-bottom: 2px solid #ccc;
             font-size: 14px; color: #555; }
        td { padding: 6px 8px; border-bottom: 1px solid #eee; }
        tr:hover td { background: #f5f8ff; }
        .icon { width: 28px; }
        .size, th.size { text-align: right; white-space: nowrap; }
        .date { white-space: nowrap; color: #555; }
        a { color: #0b57d0; text-decoration: none; }
        a:hover { text-decoration: underline; }
    </style>
</head>
<body>
<div class="time">
    <fmt:formatDate value="${now}" pattern="dd.MM.yyyy HH:mm:ss"/>
</div>
<h1><c:out value="${currentPath}"/></h1>

<c:if test="${not empty parentPath}">
    <c:url var="upUrl" value="/files">
        <c:param name="path" value="${parentPath}"/>
    </c:url>
    <a class="up" href="${upUrl}">⬆ Вверх</a>
</c:if>

<table>
    <tr>
        <th class="icon"></th>
        <th>Файл</th>
        <th class="size">Размер</th>
        <th>Дата</th>
    </tr>
    <c:forEach var="e" items="${entries}">
        <c:url var="url" value="/files">
            <c:param name="path" value="${e.path}"/>
        </c:url>
        <tr>
            <td class="icon">${e.directory ? '📁' : '📄'}</td>
            <td><a href="${url}"><c:out value="${e.name}"/></a></td>
            <td class="size">${e.size}</td>
            <td class="date">
                <fmt:formatDate value="${e.lastModified}" pattern="dd.MM.yyyy HH:mm:ss"/>
            </td>
        </tr>
    </c:forEach>
</table>
</body>
</html>