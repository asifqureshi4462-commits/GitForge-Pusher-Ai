<?php
// api/webhook.php - Real Payment Gateway Webhook Listener
header('Content-Type: application/json');
require_once __DIR__ . '/../config/app.php';
require_once __DIR__ . '/../src/Database.php';

use GhPusher\Database;

$config = require __DIR__ . '/../config/app.php';
$secret = $config['webhook_secret'];
$signature = $_SERVER['HTTP_X_WEBHOOK_SIGNATURE'] ?? $_SERVER['HTTP_X_RAZORPAY_SIGNATURE'] ?? '';
$payload = file_get_contents('php://input');

if (empty($payload)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Empty payload']);
    exit;
}

// Verify HMAC SHA256 Signature
$expectedSig = hash_hmac('sha256', $payload, $secret);
if (!hash_equals($expectedSig, $signature) && getenv('APP_ENV') !== 'development') {
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Invalid webhook signature']);
    exit;
}

$data = json_decode($payload, true);
$event = $data['event'] ?? '';
$db = Database::getConnection();

if ($event === 'subscription.activated' || $event === 'payment.captured') {
    $userId = (int)($data['user_id'] ?? 0);
    $plan = $data['plan'] ?? 'pro';
    $paymentId = $data['payment_id'] ?? 'pay_' . uniqid();

    if ($userId > 0) {
        $stmt = $db->prepare("UPDATE users SET plan = ? WHERE id = ?");
        $stmt->execute([$plan, $userId]);

        $stmtSub = $db->prepare("
            INSERT INTO subscriptions (user_id, plan, status, payment_provider, payment_id, starts_at, expires_at)
            VALUES (?, ?, 'active', 'webhook_gateway', ?, CURRENT_TIMESTAMP, datetime('now', '+30 days'))
        ");
        $stmtSub->execute([$userId, $plan, $paymentId]);
    }
}

echo json_encode(['success' => true, 'message' => 'Webhook received and processed.']);