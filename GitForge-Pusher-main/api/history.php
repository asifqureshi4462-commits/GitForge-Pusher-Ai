<?php
// api/history.php
header('Content-Type: application/json');
require_once __DIR__ . '/../src/Database.php';
require_once __DIR__ . '/../src/Auth.php';
require_once __DIR__ . '/../src/PlanManager.php';
require_once __DIR__ . '/../src/HistoryManager.php';

use GhPusher\Auth;
use GhPusher\PlanManager;
use GhPusher\HistoryManager;

$user = Auth::getUser();
if (!$user) {
    echo json_encode(['success' => false, 'error' => 'Unauthorized']);
    exit;
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $history = HistoryManager::getUserHistory($user['id']);
    echo json_encode(['success' => true, 'history' => $history]);
    exit;
}

if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?? [];

    $allowance = PlanManager::checkPushAllowance($user['id']);
    if (!$allowance['allowed'] && ($input['status'] ?? '') === 'success') {
        echo json_encode(['success' => false, 'error' => $allowance['error']]);
        exit;
    }

    $historyId = HistoryManager::logPush(
        $user['id'],
        $input['github_username'] ?? 'unknown',
        $input['repo_name'] ?? 'unknown',
        $input['branch_name'] ?? 'main',
        $input['commit_sha'] ?? null,
        $input['commit_message'] ?? '',
        (int)($input['files_count'] ?? 0),
        $input['status'] ?? 'success',
        $input['error_message'] ?? null
    );

    if (($input['status'] ?? '') === 'success') {
        PlanManager::recordSuccessfulPush(
            $user['id'],
            (int)($input['files_count'] ?? 0),
            (int)($input['bytes_uploaded'] ?? 0)
        );
    }

    echo json_encode(['success' => true, 'history_id' => $historyId]);
    exit;
}