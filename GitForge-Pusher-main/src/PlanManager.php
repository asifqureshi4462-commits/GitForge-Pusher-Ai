<?php
// src/PlanManager.php - Server-Side Usage & Plan Verification
namespace GhPusher;

use PDO;

class PlanManager {
    public static function getPlanConfig(string $planKey): array {
        $config = require __DIR__ . '/../config/app.php';
        return $config['plans'][$planKey] ?? $config['plans']['free'];
    }

    public static function getUserUsage(int $userId): array {
        $db = Database::getConnection();
        $currentMonth = date('Y-m');

        $stmt = $db->prepare("SELECT * FROM monthly_usage WHERE user_id = ? AND month_year = ?");
        $stmt->execute([$userId, $currentMonth]);
        $usage = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$usage) {
            return [
                'month_year' => $currentMonth,
                'push_count' => 0,
                'files_uploaded' => 0,
                'bytes_uploaded' => 0
            ];
        }
        return $usage;
    }

    public static function checkPushAllowance(int $userId): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT plan FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        $planKey = $user['plan'] ?? 'free';
        $plan = self::getPlanConfig($planKey);

        if ($plan['monthly_push_limit'] === -1) {
            return ['allowed' => true, 'plan' => $planKey, 'remaining' => 'Unlimited'];
        }

        $usage = self::getUserUsage($userId);
        $remaining = $plan['monthly_push_limit'] - $usage['push_count'];

        if ($remaining <= 0) {
            return [
                'allowed' => false,
                'plan' => $planKey,
                'remaining' => 0,
                'error' => 'Monthly limit reached (5/5). Upgrade to Pro for unlimited pushes and features.'
            ];
        }

        return ['allowed' => true, 'plan' => $planKey, 'remaining' => $remaining];
    }

    public static function recordSuccessfulPush(int $userId, int $fileCount, int $bytes): void {
        $db = Database::getConnection();
        $currentMonth = date('Y-m');

        $stmt = $db->prepare("
            INSERT INTO monthly_usage (user_id, month_year, push_count, files_uploaded, bytes_uploaded, updated_at)
            VALUES (?, ?, 1, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(user_id, month_year) DO UPDATE SET
                push_count = push_count + 1,
                files_uploaded = files_uploaded + excluded.files_uploaded,
                bytes_uploaded = bytes_uploaded + excluded.bytes_uploaded,
                updated_at = CURRENT_TIMESTAMP
        ");
        $stmt->execute([$userId, $currentMonth, $fileCount, $bytes]);
    }
}