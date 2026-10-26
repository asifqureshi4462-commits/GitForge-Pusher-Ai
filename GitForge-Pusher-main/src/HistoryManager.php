<?php
// src/HistoryManager.php - Push History Tracker
namespace GhPusher;

use PDO;

class HistoryManager {
    public static function logPush(
        int $userId,
        string $ghUsername,
        string $repo,
        string $branch,
        ?string $commitSha,
        string $message,
        int $fileCount,
        string $status,
        ?string $error = null
    ): int {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            INSERT INTO upload_history 
            (user_id, github_username, repo_name, branch_name, commit_sha, commit_message, files_count, status, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([$userId, $ghUsername, $repo, $branch, $commitSha, $message, $fileCount, $status, $error]);
        return (int)$db->lastInsertId();
    }

    public static function getUserHistory(int $userId, int $limit = 50): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT * FROM upload_history 
            WHERE user_id = ? 
            ORDER BY created_at DESC 
            LIMIT ?
        ");
        $stmt->bindValue(1, $userId, PDO::PARAM_INT);
        $stmt->bindValue(2, $limit, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
}