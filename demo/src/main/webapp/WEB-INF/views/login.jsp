<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Вход</title>
    <style>
        body { font-family: system-ui, "Segoe UI", Arial, sans-serif; max-width: 360px; margin: 80px auto; padding: 0 16px; }
        label { display: block; margin: 12px 0 4px; font-size: 14px; color: #555; }
        input { width: 100%; padding: 8px; box-sizing: border-box; }
        button { margin-top: 16px; padding: 8px 16px; }
        .error { color: #b00020; }
        a { color: #0b57d0; }
    </style>
</head>
<body>
<h1>Вход</h1>
<c:if test="${not empty error}"><p class="error"><c:out value="${error}"/></p></c:if>
<form method="post" action="${pageContext.request.contextPath}/login">
    <label>Логин</label>
    <input name="login" value="<c:out value='${login}'/>" required autofocus>
    <label>Пароль</label>
    <input type="password" name="password" required>
    <button type="submit">Войти</button>
</form>
<p><a href="${pageContext.request.contextPath}/register">Регистрация</a></p>
</body>
</html>