<?php
// api/user.php
header('Content-Type: application/json');
require_once __DIR__ . '/../src/Database.php';
require_once __DIR__ . '/../src/Auth.php';
require_once __DIR__ . '/../src/PlanManager.php';

use GhPusher\Auth;
use GhPusher\PlanManager;
use GhPusher\Database;

$user = Auth::getUser();
if (!$user) {
    echo json_encode(['success' => false, 'error' => 'Unauthorized']);
    exit;
}

$action = $_GET['action'] ?? 'dashboard';

if ($action === 'dashboard') {
    $usage = PlanManager::getUserUsage($user['id']);
    $allowance = PlanManager::checkPushAllowance($user['id']);
    $planConfig = PlanManager::getPlanConfig($user['plan']);

    echo json_encode([
        'success' => true,
        'user' => $user,
        'plan_config' => $planConfig,
        'usage' => $usage,
        'allowance' => $allowance
    ]);
    exit;
}

if ($action === 'check_allowance') {
    $allowance = PlanManager::checkPushAllowance($user['id']);
    echo json_encode(['success' => true, 'allowance' => $allowance]);
    exit;
}

if ($action === 'update_settings') {
    $input = json_decode(file_get_contents('php://input'), true) ?? [];
    $db = Database::getConnection();
    $stmt = $db->prepare("
        INSERT INTO user_settings (user_id, default_branch, default_commit_msg, theme, auto_exclude_secrets)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(user_id) DO UPDATE SET
            default_branch = excluded.default_branch,
            default_commit_msg = excluded.default_commit_msg,
            theme = excluded.theme,
            auto_exclude_secrets = excluded.auto_exclude_secrets
    ");
    $stmt->execute([
        $user['id'],
        $input['default_branch'] ?? 'main',
        $input['default_commit_msg'] ?? 'Deploy project via GitHub Project Pusher v2',
        $input['theme'] ?? 'dark',
        isset($input['auto_exclude_secrets']) ? (int)$input['auto_exclude_secrets'] : 1
    ]);
    echo json_encode(['success' => true]);
    exit;
}

echo json_encode(['success' => false, 'error' => 'Invalid action']);