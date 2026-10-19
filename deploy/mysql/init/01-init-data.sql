-- Initialize roles
INSERT INTO paykesho.role (id, is_default, description, name)
VALUES (1, false, 'manage everything', 'Admin'),
       (2, false, 'manage the finance data', 'Finance'),
       (3, false, 'manage everything', 'SuperAdmin'),
       (4, false, 'general viewing', 'General');

-- Initialize users
INSERT INTO paykesho.users (id, account_non_locked, deleted, email, enabled, failed_attempt, first_name, gender,
                            last_name, lock_time, password, photos, photo_path, remote_file_id)
VALUES (1, true, false, 'kaengamwamba@gmail.com', true, 0, 'Mwamba', NULL, 'Kaenga', NULL,
        '$2a$10$sbAAIiLRAZKSldC4Edye2edmj1Gps.rxpZcPj1VWhV6qqa2ncfcTi', 'upload_16434597887081365313nac.jpg', NULL, 11),
       (2, true, false, 'sokofella@gmail.com', true, 0, 'Innocent', NULL, 'Soko', NULL,
        '$2a$10$B6ATwTmF8r9SM.xoOiXd9.fnzqUXaBHR1cQba7yP5rVL6lP.cn6Wq', 'upload_3559234594436994107ZRA-logo-01.png',
        NULL, 44);

-- Initialize user roles
INSERT INTO paykesho.user_roles (user_id, role_id)
VALUES (1, 1),
       (2, 1),
       (1, 2),
       (2, 2),
       (1, 3),
       (2, 3),
       (1, 4),
       (2, 4);

-- Initialize countries
INSERT INTO paykesho.countries (id, code, name)
VALUES (249, 'ZM', 'ZAMBIA');

-- Initialize states
INSERT INTO paykesho.states (id, name, country_id)
VALUES (1, 'LUSAKA', 249);

-- Initialize settings
INSERT INTO paykesho.settings (setting_key, category, value)
VALUES ('CLIENT_VERIFY_CONTENT', 'MAIL_TEMPLATES',
        '<span style="font-size:16px;"><span style="font-size:14px;"><font face="Tahoma">Dear [[name]]<br>'),
       ('CLIENT_VERIFY_SUBJECT', 'MAIL_TEMPLATES', 'Paykesho Team - Please verify your registration'),
       ('COPYRIGHT', 'GENERAL', 'Copyright (C) 2024 Paykesho'),
       ('CURRENCY_ID', 'CURRENCY', '1'),
       ('CURRENCY_SYMBOL', 'CURRENCY', 'K'),
       ('CURRENCY_SYMBOL_POSITION', 'CURRENCY', 'before price'),
       ('DECIMAL_DIGITS', 'CURRENCY', '2'),
       ('DECIMAL_POINT_TYPE', 'CURRENCY', 'POINT'),
       ('MAIL_FROM', 'MAIL_SERVER', 'amukenam1@gmail.com'),
       ('MAIL_HOST', 'MAIL_SERVER', 'smtp.gmail.com'),
       ('MAIL_PASSWORD', 'MAIL_SERVER', 'dczrrhysikvranvc'),
       ('MAIL_PORT', 'MAIL_SERVER', '587'),
       ('MAIL_SENDER_NAME', 'MAIL_SERVER', 'Paykesho'),
       ('MAIL_USERNAME', 'MAIL_SERVER', 'amukenam1@gmail.com'),
       ('MEMBER_ADMIN_CREATION_CONTENT', 'MAIL_TEMPLATES',
        'Dear [[name]]<div><br></div><div>This email is to confirm that your account has been created. Kindly see details below</div><div><br></div><div>Link: [[link]]...'),
       ('MEMBER_ADMIN_CREATION_SUBJECT', 'MAIL_TEMPLATES', 'Paykesho Account Creation'),
       ('PAYPAL_API_BASE_URL', 'PAYMENT', 'https://api-m.sandbox.paypal.com'),
       ('PAYPAL_API_CLIENT_ID', 'PAYMENT',
        'ATFF09-q_rcJChVLw9agTroQ5vQOo6k63mi3tENCD51FNxaDdIAkSxT77s_aCY1mlI-pWQil81VGUZb8'),
       ('PAYPAL_API_CLIENT_SECRET', 'PAYMENT',
        'EJwXlEPGd1-GHgjFEdPYSEGO-t6m2fziFDkoFbo2nU4vKLnkNnLMBhKQC8UoAOGvXTWG9oyCBd02ubq8'),
       ('SITE_LOGO', 'GENERAL', '/site_logo/chazlogo.png'),
       ('SITE_NAME', 'GENERAL', 'Paykesho'),
       ('SMTP_AUTH', 'MAIL_SERVER', 'true'),
       ('SMTP_SECURED', 'MAIL_SERVER', 'true'),
       ('THOUSAND_POINT_TYPE', 'CURRENCY', 'COMMA');