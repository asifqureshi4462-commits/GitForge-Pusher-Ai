<?php
// src/Database.php - PDO Connection & Auto-Migration Handler
namespace GhPusher;

use PDO;
use PDOException;

class Database {
    private static ?PDO $instance = null;

    public static function getConnection(): PDO {
        if (self::$instance === null) {
            $config = require __DIR__ . '/../config/database.php';
            try {
                if ($config['driver'] === 'sqlite') {
                    $dbDir = dirname($config['sqlite']['path']);
                    if (!is_dir($dbDir)) {
                        mkdir($dbDir, 0755, true);
                    }
                    self::$instance = new PDO('sqlite:' . $config['sqlite']['path']);
                    self::$instance->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
                    self::$instance->exec('PRAGMA foreign_keys = ON;');
                } else {
                    $dsn = sprintf(
                        'mysql:host=%s;port=%s;dbname=%s;charset=%s',
                        $config['mysql']['host'],
                        $config['mysql']['port'],
                        $config['mysql']['database'],
                        $config['mysql']['charset']
                    );
                    self::$instance = new PDO($dsn, $config['mysql']['username'], $config['mysql']['password'], [
                        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
                    ]);
                }
                self::ensureSchema();
            } catch (PDOException $e) {
                die(json_encode(['success' => false, 'error' => 'Database connection failed: ' . $e->getMessage()]));
            }
        }
        return self::$instance;
    }

    private static function ensureSchema(): void {
        $schemaFile = __DIR__ . '/../database/schema.sql';
        if (file_exists($schemaFile)) {
            $sql = file_get_contents($schemaFile);
            self::$instance->exec($sql);
            
            // Seed default admin if table is brand new
            $stmt = self::$instance->query("SELECT COUNT(*) as cnt FROM users");
            if ($stmt->fetch()['cnt'] == 0) {
                $adminPass = password_hash('Admin@2026', PASSWORD_BCRYPT);
                $stmt = self::$instance->prepare(
                    "INSERT INTO users (username, email, password_hash, role, plan) VALUES (?, ?, ?, 'admin', 'pro')"
                );
                $stmt->execute(['admin', 'admin@ghpusher.local', $adminPass]);
            }
        }
    }
}