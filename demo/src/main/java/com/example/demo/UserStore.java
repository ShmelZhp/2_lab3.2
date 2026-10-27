package com.example.demo;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.exception.ConstraintViolationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class UserStore {

    private static final Logger LOG =
            Logger.getLogger(UserStore.class.getName());

    private static final Path BASE = Paths.get(
            System.getProperty(
                    "filemanager.dir",
                    "/var/lib/tomcat10/filemanager"
            )
    ).toAbsolutePath().normalize();

    public static final Path HOMES_ROOT = BASE.resolve("homes");

    private static final Path DB_CONFIG =
            BASE.resolve("db.properties");

    private static final Pattern LOGIN =
            Pattern.compile("[a-z0-9_-]{3,32}");

    private static final SecureRandom RND = new SecureRandom();

    private static final UserStore INSTANCE = new UserStore();

    private volatile SessionFactory sessionFactory;

    private UserStore() {
    }

    public static UserStore get() {
        return INSTANCE;
    }

    public static String normalize(String login) {
        return login == null
                ? ""
                : login.trim().toLowerCase(Locale.ROOT);
    }

    public String register(
            String rawLogin,
            String password,
            String email) {

        String login = normalize(rawLogin);

        if (!LOGIN.matcher(login).matches()) {
            return "Логин: 3-32 символа, латинские буквы, цифры, _ и -";
        }

        if (password == null || password.length() < 4) {
            return "Пароль должен быть не короче 4 символов";
        }

        if (email == null ||
                !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return "Некорректный email";
        }

        try {
            byte[] salt = new byte[16];
            RND.nextBytes(salt);

            String saltB64 = b64(salt);
            String hashB64 = b64(hash(password, salt));

            Files.createDirectories(homeDir(login));

            UserEntity user = new UserEntity(
                    login,
                    saltB64,
                    hashB64,
                    email.trim()
            );

            try (Session session = getSessionFactory().openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    session.persist(user);
                    tx.commit();
                    return null;
                } catch (RuntimeException e) {
                    if (tx.isActive()) {
                        try {
                            tx.rollback();
                        } catch (RuntimeException rollbackError) {
                            e.addSuppressed(rollbackError);
                        }
                    }

                    if (isDuplicateLogin(e)) {
                        return "Такой логин уже занят";
                    }

                    LOG.log(
                            Level.SEVERE,
                            "Ошибка Hibernate при регистрации " + login,
                            e
                    );

                    return "Ошибка базы данных: " + rootMessage(e);
                }
            }
        } catch (IOException e) {
            LOG.log(
                    Level.SEVERE,
                    "Ошибка файловой системы для пользователя " + login,
                    e
            );
            return "Ошибка сервера: " + e.getMessage();
        } catch (GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "Ошибка хеширования пароля", e);
            return "Ошибка сервера при обработке пароля";
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Не удалось зарегистрировать " + login, e);
            return "Ошибка базы данных: " + rootMessage(e);
        }
    }

    public boolean authenticate(
            String rawLogin,
            String password) {

        String login = normalize(rawLogin);

        if (password == null || !LOGIN.matcher(login).matches()) {
            return false;
        }

        try (Session session = getSessionFactory().openSession()) {
            UserEntity user = session.createQuery(
                            "FROM UserEntity WHERE login = :login",
                            UserEntity.class
                    )
                    .setParameter("login", login)
                    .uniqueResult();

            if (user == null) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(user.getSalt());
            byte[] expected =
                    Base64.getDecoder().decode(user.getPasswordHash());

            return MessageDigest.isEqual(expected, hash(password, salt));

        } catch (GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "Ошибка проверки пароля", e);
            throw new IllegalStateException(
                    "Ошибка при проверке пароля", e
            );
        } catch (RuntimeException e) {
            LOG.log(
                    Level.SEVERE,
                    "Ошибка Hibernate при входе пользователя " + login,
                    e
            );
            throw new IllegalStateException(
                    "Ошибка базы данных: " + rootMessage(e), e
            );
        }
    }

    public Path homeDir(String login) {
        return HOMES_ROOT.resolve(login);
    }

    private SessionFactory getSessionFactory() {
        SessionFactory result = sessionFactory;

        if (result == null) {
            synchronized (this) {
                result = sessionFactory;

                if (result == null) {
                    result = buildSessionFactory();
                    sessionFactory = result;
                }
            }
        }

        return result;
    }

    private SessionFactory buildSessionFactory() {
        Properties db = new Properties();

        try (var reader = Files.newBufferedReader(
                DB_CONFIG, StandardCharsets.UTF_8)) {
            db.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось прочитать настройки БД: " + DB_CONFIG, e
            );
        }

        String url = required(db, "url");
        String user = required(db, "user");
        String password = required(db, "password");

        Map<String, Object> settings = new HashMap<>();
        settings.put(
                "hibernate.connection.driver_class",
                "com.mysql.cj.jdbc.Driver"
        );
        settings.put("hibernate.connection.url", url);
        settings.put("hibernate.connection.username", user);
        settings.put("hibernate.connection.password", password);
        settings.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        settings.put("hibernate.hbm2ddl.auto", "update");
        settings.put("hibernate.show_sql", "false");
        settings.put("hibernate.format_sql", "false");

        StandardServiceRegistry registry =
                new StandardServiceRegistryBuilder()
                        .applySettings(settings)
                        .build();

        try {
            return new MetadataSources(registry)
                    .addAnnotatedClass(UserEntity.class)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (RuntimeException e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw new IllegalStateException(
                    "Не удалось запустить Hibernate. Проверьте MySQL "
                            + "и db.properties.",
                    e
            );
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "В файле " + DB_CONFIG + " отсутствует параметр " + key
            );
        }

        return value.trim();
    }

    private static boolean isDuplicateLogin(Throwable error) {
        Throwable current = error;

        while (current != null) {
            if (current instanceof ConstraintViolationException) {
                ConstraintViolationException cve =
                        (ConstraintViolationException) current;

                if (cve.getConstraintName() == null ||
                        cve.getConstraintName().contains("users_login") ||
                        cve.getConstraintName().contains("uk_users_login")) {
                    return true;
                }
            }

            if (current instanceof SQLException) {
                SQLException sql = (SQLException) current;

                if (sql.getErrorCode() == 1062 ||
                        (sql.getSQLState() != null &&
                                sql.getSQLState().startsWith("23"))) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;

        while (current.getCause() != null &&
                current.getCause() != current) {
            current = current.getCause();
        }

        return current.getMessage() == null
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }

    private static byte[] hash(
            String password,
            byte[] salt) throws GeneralSecurityException {

        var spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                120_000,
                256
        );

        try {
            return javax.crypto.SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
