package com.example;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@WebServlet("/files")
public class FileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Читаем URL-параметр path
        String pathParam = request.getParameter("path");
        if (pathParam == null || pathParam.trim().isEmpty()) {
            pathParam = System.getProperty("user.home");
        }

        File currentDir = new File(pathParam);

        // Проверяем существование пути
        if (!currentDir.exists()) {
            currentDir = new File(System.getProperty("user.home"));
        } else if (currentDir.isFile()) {
            currentDir = currentDir.getParentFile();
        }

        // 2. Родительский каталог для ссылки "Вверх"
        File parentDir = currentDir.getParentFile();

        // 3. Форматируем текущую дату и время генерации страницы (как на скриншоте)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String currentTime = LocalDateTime.now().format(formatter);

        // 4. Передаем атрибуты в JSP
        request.setAttribute("currentPath", currentDir.getAbsolutePath());
        request.setAttribute("parentPath", parentDir != null ? parentDir.getAbsolutePath() : null);

        File[] filesArr = currentDir.listFiles();
        if (filesArr != null) {
            // Сортируем: сначала директории, потом файлы
            Arrays.sort(filesArr, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
        }

        request.setAttribute("files", filesArr);
        request.setAttribute("currentTime", currentTime);

        // 5. Перенаправляем на index.jsp
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}