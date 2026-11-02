<?php
// config/database.php - Database Configuration
return [
    'driver'   => getenv('DB_DRIVER') ?: 'sqlite', // 'sqlite' or 'mysql'
    'sqlite'   => [
        'path' => __DIR__ . '/../database/app.sqlite'
    ],
    'mysql'    => [
        'host'     => getenv('DB_HOST') ?: '127.0.0.1',
        'port'     => getenv('DB_PORT') ?: 3306,
        'database' => getenv('DB_NAME') ?: 'gh_pusher_v2',
        'username' => getenv('DB_USER') ?: 'root',
        'password' => getenv('DB_PASS') ?: '',
        'charset'  => 'utf8mb4'
    ]
];