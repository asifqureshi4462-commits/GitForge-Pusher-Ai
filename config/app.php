<?php
// config/app.php - Central Application Configuration
return [
    'app_name' => 'GitHub Project Pusher v2.0',
    'app_url'  => 'http://127.0.0.1:8000',
    'version'  => '2.0.0',
    
    // Feature gating & Limits
    'plans' => [
        'free' => [
            'name' => 'Free Community',
            'price_inr' => 0,
            'monthly_push_limit' => 5,
            'max_file_size_mb' => 25,
            'can_auto_scan' => true,
            'can_detect_changes' => false,
            'can_create_repos' => false,
            'can_use_history' => false,
        ],
        'pro' => [
            'name' => 'Pro Developer',
            'price_inr' => 99,
            'monthly_push_limit' => -1, // -1 represents unlimited
            'max_file_size_mb' => 100,
            'can_auto_scan' => true,
            'can_detect_changes' => true,
            'can_create_repos' => true,
            'can_use_history' => true,
        ]
    ],

    // Webhook secret for payment gateway integration (Razorpay / Stripe / LemonSqueezy)
    'webhook_secret' => getenv('PAYMENT_WEBHOOK_SECRET') ?: 'whsec_github_pusher_dev_secret_key_2026',
];