INSERT INTO users (email, password, role)
VALUES
    (
        'admin@pm.com',
        '$2y$10$17by3kSLqaWYjZKVv.Qgru/zP8Rf0mxa21uYGTQ4HMfGtBEiThlGS',
        'ADMIN'
    ),
    (
        'receptionist@pm.com',
        '$2y$10$17by3kSLqaWYjZKVv.Qgru/zP8Rf0mxa21uYGTQ4HMfGtBEiThlGS',
        'RECEPTIONIST'
    ),
    (
        'doctor@pm.com',
        '$2y$10$17by3kSLqaWYjZKVv.Qgru/zP8Rf0mxa21uYGTQ4HMfGtBEiThlGS',
        'DOCTOR'
    )
ON CONFLICT (email) DO NOTHING;
