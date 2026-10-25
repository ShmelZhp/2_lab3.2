package com.example.demo;

import java.io.File;
import java.util.Date;

public class FileEntry {
    private final String name;
    private final String path;
    private final boolean directory;
    private final String size;
    private final Date lastModified;

    public FileEntry(File f) {
        this.directory = f.isDirectory();
        this.name = f.getName() + (directory ? "/" : "");
        this.path = f.getAbsolutePath();
        this.size = directory ? "" : humanSize(f.length());
        this.lastModified = new Date(f.lastModified());
    }

    private static String humanSize(long b) {
        if (b < 1024) return b + " B";
        String[] u = {"KB", "MB", "GB", "TB"};
        double v = b;
        int i = -1;
        do { v /= 1024; i++; } while (v >= 1024 && i < u.length - 1);
        return String.format("%.1f %s", v, u[i]);
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public boolean isDirectory() { return directory; }
    public String getSize() { return size; }
    public Date getLastModified() { return lastModified; }
}