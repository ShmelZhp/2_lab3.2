package com.example.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FileBrowserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String p = readPath(req);
        File target = (p == null || p.isBlank())
                ? new File(System.getProperty("user.home"))
                : new File(p);

        if (!target.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Не найдено: " + p);
            return;
        }
        target = target.getCanonicalFile();

        if (target.isFile()) {
            sendFile(target, resp);
            return;
        }

        File[] children = target.listFiles();
        if (children == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Нет доступа к каталогу");
            return;
        }

        List<FileEntry> entries = new ArrayList<>();
        for (File c : children) entries.add(new FileEntry(c));
        entries.sort(Comparator.comparing(FileEntry::isDirectory).reversed()
                .thenComparing(e -> e.getName().toLowerCase()));

        File parent = target.getParentFile();
        req.setAttribute("now", new Date());
        req.setAttribute("currentPath", target.getAbsolutePath());
        req.setAttribute("parentPath", parent == null ? null : parent.getAbsolutePath());
        req.setAttribute("entries", entries);

        req.getRequestDispatcher("/WEB-INF/jsp/files.jsp").forward(req, resp);
    }

    private void sendFile(File f, HttpServletResponse resp) throws IOException {
        String encoded = URLEncoder.encode(f.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        resp.setContentType("application/octet-stream");
        resp.setContentLengthLong(f.length());
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        Files.copy(f.toPath(), resp.getOutputStream());
    }

    private static String readPath(HttpServletRequest req) {
        String q = req.getQueryString();
        if (q == null) return null;
        for (String pair : q.split("&")) {
            if (pair.startsWith("path=")) {
                return URLDecoder.decode(pair.substring(5), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}