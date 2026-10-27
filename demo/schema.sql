CREATE DATABASE IF NOT EXISTS filemanager CHARACTER SET utf8mb4;
USE filemanager;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    login         VARCHAR(32)  NOT NULL,
    salt          VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_login (login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;