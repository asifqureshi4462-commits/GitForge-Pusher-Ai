CREATE DATABASE IF NOT EXISTS github_project_pusher
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE github_project_pusher;

CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    github_username VARCHAR(100) NULL,
    email VARCHAR(190) NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    plan ENUM('free','pro') NOT NULL DEFAULT 'free',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    status ENUM('active','cancelled','expired','pending') NOT NULL DEFAULT 'pending',
    provider VARCHAR(50) NULL,
    external_id VARCHAR(190) NULL,
    starts_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS usage (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    period_key CHAR(7) NOT NULL,
    push_count INT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE KEY user_period (user_id, period_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS upload_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    repository VARCHAR(190) NOT NULL,
    branch_name VARCHAR(190) NOT NULL,
    commit_message VARCHAR(255) NOT NULL,
    file_count INT UNSIGNED NOT NULL DEFAULT 0,
    status ENUM('success','failed','partial') NOT NULL DEFAULT 'failed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
