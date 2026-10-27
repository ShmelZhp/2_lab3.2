# Файловый менеджер: сервлеты, JSP, авторизация, MySQL (JDBC)

Spring Boot 3.3 (war) + сервлеты + JSP. Пользователи хранятся в MySQL, доступ через JDBC.
Сессии сохраняются на диск (Tomcat PersistentManager + FileStore, см. src/main/webapp/META-INF/context.xml).

## Запуск (Tomcat 10.1, Java 17)

1. Создайте БД и пользователя MySQL (структура таблицы в schema.sql, приложение создаёт её само).
2. Создайте на сервере файл /var/lib/tomcat10/filemanager/db.properties:

        url=jdbc:mysql://127.0.0.1:3306/filemanager?characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true
        user=<пользователь>
        password=<пароль>

3. Соберите: ./mvnw clean package
4. Разверните target/demo.war на Tomcat с путём контекста /demo.