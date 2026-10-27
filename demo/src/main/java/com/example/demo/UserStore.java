package com.example.demo;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Пользователи хранятся в MySQL (таблица users), доступ через чистый JDBC.
 * Параметры подключения читаются из файла db.properties, который лежит на сервере
 * рядом с домашними папками, поэтому пароль от БД не попадает в репозиторий.
 */
public final class UserStore {

    private static final Logger LOG = Logger.getLogger(UserStore.class.getName());

    private static final Path BASE = Paths.get(System.getProperty("filemanager.dir",
            "/var/lib/tomcat10/filemanager")).toAbsolutePath().normalize();
    /** Родительская папка для домашних папок: .../filemanager/homes */
    public static final Path HOMES_ROOT = BASE.resolve("homes");
    /** Файл с параметрами подключения к БД (url, user, password). */
    private static final Path DB_CONFIG = BASE.resolve("db.properties");

    // Только эти символы: в логине не будет "..", "/" и "\"
    private static final Pattern LOGIN = Pattern.compile("[a-z0-9_-]{3,32}");
    private static final SecureRandom RND = new SecureRandom();

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS users ("
                    + " id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + " login VARCHAR(32) NOT NULL,"
                    + " salt VARCHAR(64) NOT NULL,"
                    + " password_hash VARCHAR(128) NOT NULL,"
                    + " email VARCHAR(255) NOT NULL,"
                    + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " UNIQUE KEY uk_users_login (login)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    private static final String INSERT_USER =
            "INSERT INTO users (login, salt, password_hash, email) VALUES (?, ?, ?, ?)";
    private static final String SELECT_USER =
            "SELECT salt, password_hash FROM users WHERE login = ?";

    private static final UserStore INSTANCE = new UserStore();

    private Properties dbConfig;   // читается из db.properties при первом обращении к БД
    private boolean schemaReady;   // таблица users уже проверена/создана

    private UserStore() {
        // Ничего не делаем здесь: если БД недоступна при старте приложения,
        // это не должно ломать загрузку класса. Подключаемся лениво.
    }

    public static UserStore get() {
        return INSTANCE;
    }

    /** Логин без учёта регистра: так Bob и bob не создадут две разные папки. */
    public static String normalize(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }

    /** Возвращает текст ошибки или null, если регистрация прошла. */
    public String register(String rawLogin, String password, String email) {
        String login = normalize(rawLogin);
        if (!LOGIN.matcher(login).matches()) {
            return "Логин: 3-32 символа, латинские буквы, цифры, _ и -";
        }
        if (password == null || password.length() < 4) {
            return "Пароль должен быть не короче 4 символов";
        }
        if (email == null || !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return "Некорректный email";
        }

        try {
            byte[] salt = new byte[16];
            RND.nextBytes(salt);
            String saltB64 = b64(salt);
            String hashB64 = b64(hash(password, salt));

            Files.createDirectories(homeDir(login));

            // Уникальность логина гарантирует UNIQUE-индекс в БД, а не проверка в коде:
            // так два одновременных запроса не создадут дубликат.
            try (Connection c = connect();
                 PreparedStatement ps = c.prepareStatement(INSERT_USER)) {
                ps.setString(1, login);
                ps.setString(2, saltB64);
                ps.setString(3, hashB64);
                ps.setString(4, email.trim());
                ps.executeUpdate();
            }
            return null;
        } catch (SQLIntegrityConstraintViolationException e) {
            return "Такой логин уже занят";
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {          // ER_DUP_ENTRY
                return "Такой логин уже занят";
            }
            LOG.log(Level.SEVERE, "Ошибка БД при регистрации пользователя " + login, e);
            return "Ошибка базы данных: " + e.getMessage();
        } catch (IOException | GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "Ошибка при регистрации пользователя " + login, e);
            return "Ошибка сервера: " + e.getMessage();
        }
    }

    public boolean authenticate(String rawLogin, String password) {
        if (password == null) return false;
        String login = normalize(rawLogin);
        if (!LOGIN.matcher(login).matches()) return false;

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(SELECT_USER)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                byte[] salt = Base64.getDecoder().decode(rs.getString("salt"));
                byte[] expected = Base64.getDecoder().decode(rs.getString("password_hash"));
                return MessageDigest.isEqual(expected, hash(password, salt));
            }
        } catch (SQLException | GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "Ошибка при проверке пароля пользователя " + login, e);
            throw new IllegalStateException("Ошибка базы данных: " + e.getMessage(), e);
        }
    }

    /** Домашняя папка пользователя. Передавать сюда нужно уже проверенный логин. */
    public Path homeDir(String login) {
        return HOMES_ROOT.resolve(login);
    }

    // ---------------------------------------------------------------- JDBC

    private Connection connect() throws SQLException {
        Properties cfg = config();
        Connection c = DriverManager.getConnection(
                cfg.getProperty("url"), cfg.getProperty("user"), cfg.getProperty("password"));
        try {
            ensureSchema(c);
            return c;
        } catch (SQLException | RuntimeException e) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // основная ошибка важнее
            }
            throw e;
        }
    }

    /** Читает db.properties один раз. Если файла нет, при следующем запросе попробует снова. */
    private synchronized Properties config() throws SQLException {
        if (dbConfig != null) return dbConfig;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("В приложении нет драйвера MySQL (mysql-connector-j)", e);
        }

        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(DB_CONFIG, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            throw new SQLException("Не удалось прочитать файл настроек БД " + DB_CONFIG + " (" + e + ")", e);
        }
        for (String key : new String[] {"url", "user", "password"}) {
            String v = p.getProperty(key);
            if (v == null) {
                throw new SQLException("В файле " + DB_CONFIG + " нет параметра " + key);
            }
            p.setProperty(key, v.trim());
        }
        dbConfig = p;
        return dbConfig;
    }

    /** Создаёт таблицу users, если её ещё нет (выполняется один раз за запуск приложения). */
    private synchronized void ensureSchema(Connection c) throws SQLException {
        if (schemaReady) return;
        try (Statement st = c.createStatement()) {
            st.executeUpdate(CREATE_TABLE);
        }
        schemaReady = true;
    }

    // ------------------------------------------------------------- пароли

    private static byte[] hash(String password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    private static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }
}