<?php
// api/auth.php
header('Content-Type: application/json');
require_once __DIR__ . '/../src/Database.php';
require_once __DIR__ . '/../src/Auth.php';

use GhPusher\Auth;

$action = $_GET['action'] ?? '';
$input = json_decode(file_get_contents('php://input'), true) ?? [];

if ($action === 'register') {
    $res = Auth::register($input['username'] ?? '', $input['email'] ?? '', $input['password'] ?? '');
    echo json_encode($res);
    exit;
}

if ($action === 'login') {
    $res = Auth::login($input['login'] ?? '', $input['password'] ?? '');
    echo json_encode($res);
    exit;
}

if ($action === 'logout') {
    Auth::logout();
    echo json_encode(['success' => true]);
    exit;
}

if ($action === 'me') {
    $user = Auth::getUser();
    echo json_encode(['success' => true, 'user' => $user]);
    exit;
}

echo json_encode(['success' => false, 'error' => 'Invalid auth endpoint']);