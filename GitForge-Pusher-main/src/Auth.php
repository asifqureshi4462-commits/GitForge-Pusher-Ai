<?php
// src/Auth.php - User Session & Role Manager
namespace GhPusher;

use PDO;

class Auth {
    public static function startSession(): void {
        if (session_status() === PHP_SESSION_NONE) {
            session_set_cookie_params([
                'lifetime' => 86400 * 7,
                'path' => '/',
                'httponly' => true,
                'samesite' => 'Lax'
            ]);
            session_start();
        }
    }

    public static function getUser(): ?array {
        self::startSession();
        if (!isset($_SESSION['user_id'])) {
            return null;
        }

        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT id, username, email, role, plan, created_at FROM users WHERE id = ?");
        $stmt->execute([$_SESSION['user_id']]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$user) {
            self::logout();
            return null;
        }
        return $user;
    }

    public static function register(string $username, string $email, string $password): array {
        $db = Database::getConnection();

        // Validate
        if (strlen($username) < 3 || strlen($password) < 6 || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return ['success' => false, 'error' => 'Invalid username, email, or password length (min 6 chars).'];
        }

        $stmt = $db->prepare("SELECT id FROM users WHERE username = ? OR email = ?");
        $stmt->execute([$username, $email]);
        if ($stmt->fetch()) {
            return ['success' => false, 'error' => 'Username or Email already exists.'];
        }

        $hash = password_hash($password, PASSWORD_BCRYPT);
        $stmt = $db->prepare("INSERT INTO users (username, email, password_hash, role, plan) VALUES (?, ?, ?, 'user', 'free')");
        $stmt->execute([$username, $email, $hash]);
        $userId = $db->lastInsertId();

        // Init settings
        $stmtSettings = $db->prepare("INSERT INTO user_settings (user_id) VALUES (?)");
        $stmtSettings->execute([$userId]);

        $_SESSION['user_id'] = $userId;
        return ['success' => true, 'user' => self::getUser()];
    }

    public static function login(string $usernameOrEmail, string $password): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT * FROM users WHERE username = ? OR email = ?");
        $stmt->execute([$usernameOrEmail, $usernameOrEmail]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$user || !password_verify($password, $user['password_hash'])) {
            return ['success' => false, 'error' => 'Invalid login credentials.'];
        }

        self::startSession();
        $_SESSION['user_id'] = $user['id'];
        return ['success' => true, 'user' => self::getUser()];
    }

    public static function logout(): void {
        self::startSession();
        unset($_SESSION['user_id']);
        session_destroy();
    }
}