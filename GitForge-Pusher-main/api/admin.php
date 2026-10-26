<?php
// api/admin.php - Protected Admin Management Dashboard
header('Content-Type: application/json');
require_once __DIR__ . '/../src/Database.php';
require_once __DIR__ . '/../src/Auth.php';

use GhPusher\Auth;
use GhPusher\Database;

$user = Auth::getUser();
if (!$user || $user['role'] !== 'admin') {
    http_response_code(403);
    echo json_encode(['success' => false, 'error' => 'Forbidden: Administrator credentials required.']);
    exit;
}

$db = Database::getConnection();

// System Statistics
$stats = [
    'total_users'     => (int)$db->query("SELECT COUNT(*) FROM users")->fetchColumn(),
    'free_users'      => (int)$db->query("SELECT COUNT(*) FROM users WHERE plan = 'free'")->fetchColumn(),
    'pro_users'       => (int)$db->query("SELECT COUNT(*) FROM users WHERE plan = 'pro'")->fetchColumn(),
    'total_pushes'    => (int)$db->query("SELECT COUNT(*) FROM upload_history")->fetchColumn(),
    'success_pushes'  => (int)$db->query("SELECT COUNT(*) FROM upload_history WHERE status = 'success'")->fetchColumn(),
    'failed_pushes'   => (int)$db->query("SELECT COUNT(*) FROM upload_history WHERE status != 'success'")->fetchColumn(),
    'recent_pushes'   => $db->query("SELECT h.*, u.username as account_user FROM upload_history h JOIN users u ON u.id = h.user_id ORDER BY h.created_at DESC LIMIT 15")->fetchAll(PDO::FETCH_ASSOC),
    'recent_users'    => $db->query("SELECT id, username, email, role, plan, created_at FROM users ORDER BY created_at DESC LIMIT 10")->fetchAll(PDO::FETCH_ASSOC)
];

echo json_encode(['success' => true, 'stats' => $stats]);