package com.example.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FilesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String pathParameter = request.getParameter("path");
        if (pathParameter == null || pathParameter.isEmpty()) {
            String home = System.getProperty("user.home");
            pathParameter = (home != null && new File(home).isDirectory()) ? home : "/";
        }

        File target = new File(pathParameter).getCanonicalFile();

        if (!target.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Путь не существует");
            return;
        }

        // Ссылка на файл в JSP ведет на /files?path=..., поэтому файл отдаем на скачивание.
        if (target.isFile()) {
            sendFile(target, response);
            return;
        }

        File[] files = target.listFiles();
        if (files == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Нет доступа к папке");
            return;
        }

        // Сначала папки, затем файлы.
        Arrays.sort(files, Comparator.comparing(File::isFile)
                .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        List<FileEntry> entries = new ArrayList<>();
        for (File f : files) {
            entries.add(new FileEntry(f));
        }

        File parent = target.getParentFile();

        request.setAttribute("currentPath", target.getPath());
        request.setAttribute("parentPath", parent != null ? parent.getPath() : null);
        request.setAttribute("entries", entries);
        request.setAttribute("now", new Date());

        request.getRequestDispatcher("/WEB-INF/views/files.jsp").forward(request, response);
    }

    private void sendFile(File file, HttpServletResponse response) throws IOException {
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        response.setContentType("application/octet-stream");
        response.setContentLengthLong(file.length());
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedName);

        Files.copy(file.toPath(), response.getOutputStream());
    }

    /** Строка таблицы для JSP: поля доступны как ${e.name}, ${e.path} и т.д. */
    public static class FileEntry {
        private final File file;

        public FileEntry(File file) {
            this.file = file;
        }

        public String getName() {
            return file.isDirectory() ? file.getName() + "/" : file.getName();
        }

        public String getPath() {
            return file.getPath();
        }

        public boolean isDirectory() {
            return file.isDirectory();
        }

        public String getSize() {
            return file.isFile() ? file.length() + " B" : "";
        }

        public Date getLastModified() {
            return new Date(file.lastModified());
        }
    }
}