<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Файловый менеджер</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            color: #000;
        }
        .time {
            font-size: 14px;
            margin-bottom: 20px;
        }
        .path {
            font-size: 28px;
            font-weight: bold;
            font-family: serif;
            margin-bottom: 10px;
        }
        hr {
            border: 0;
            border-top: 1px solid #ccc;
            margin-bottom: 15px;
        }
        .up-link {
            margin-bottom: 15px;
            font-size: 16px;
        }
        .up-link a {
            color: #551a8b;
            text-decoration: underline;
        }
        table {
            border-collapse: collapse;
            margin-top: 10px;
        }
        th, td {
            padding: 4px 12px;
            text-align: left;
            font-size: 15px;
        }
        th {
            font-weight: bold;
        }
        a {
            color: #1a0dab;
            text-decoration: underline;
        }
        .icon {
            margin-right: 4px;
        }
    </style>
</head>
<body>

    <!-- 1. Дата и время генерации страницы -->
    <div class="time">${currentTime}</div>

    <!-- 2. Путь к текущей директории -->
    <div class="path">${currentPath}</div>

    <hr/>

    <!-- 3. Кнопка/ссылка Вверх -->
    <c:if test="${not empty parentPath}">
        <div class="up-link">
            <a href="<c:url value='/files'><c:param name='path' value='${parentPath}'/></c:url>">
                🔝 Вверх
            </a>
        </div>
    </c:if>

    <!-- 4. Содержимое текущей директории -->
    <table>
        <thead>
            <tr>
                <th>Файл</th>
                <th>Размер</th>
                <th>Дата</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="f" items="${files}">
                <tr>
                    <td>
                        <c:choose>
                            <c:when test="${f.directory}">
                                📁 <a href="<c:url value='/files'><c:param name='path' value='${f.absolutePath}'/></c:url>">
                                    ${f.name}/
                                </a>
                            </c:when>
                            <c:otherwise>
                                📄 <a href="<c:url value='/download'><c:param name='path' value='${f.absolutePath}'/></c:url>">
                                    ${f.name}
                                </a>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:if test="${!f.directory}">
                            ${f.length()} B
                        </c:if>
                    </td>
                    <td>
                        <jsp:useBean id="dateValue" class="java.util.Date"/>
                        <jsp:setProperty name="dateValue" property="time" value="${f.lastModified()}"/>
                        <fmt:formatDate value="${dateValue}" pattern="M/d/yy, h:mm:ss a"/>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

</body>
</html>