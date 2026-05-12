-- Seed additional demo users and staff accounts for local development / presentation.
-- Password for all inserted accounts: 123456

SET @DEMO_BCRYPT := '$2a$10$3F0M4TngaCFogHFb5hPn8O5cXUvvf8B56Hv/FGs8xzEPNo5xKV7Zm';

-- Additional USER accounts
INSERT INTO sys_user (username, password, nickname, phone, email, avatar_url, role, enabled)
SELECT 'user2',
       @DEMO_BCRYPT,
       'User 2',
       '13900000022',
       'user2@example.com',
       '/uploads/avatars/user2.png',
       'USER',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'user2'
       OR phone = '13900000022'
       OR email = 'user2@example.com'
);

INSERT INTO sys_user (username, password, nickname, phone, email, avatar_url, role, enabled)
SELECT 'user3',
       @DEMO_BCRYPT,
       'User 3',
       '13900000023',
       'user3@example.com',
       '/uploads/avatars/user3.png',
       'USER',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'user3'
       OR phone = '13900000023'
       OR email = 'user3@example.com'
);

INSERT INTO sys_user (username, password, nickname, phone, email, avatar_url, role, enabled)
SELECT 'user4',
       @DEMO_BCRYPT,
       'User 4',
       '13900000024',
       'user4@example.com',
       '/uploads/avatars/user4.png',
       'USER',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'user4'
       OR phone = '13900000024'
       OR email = 'user4@example.com'
);

INSERT INTO sys_user (username, password, nickname, phone, email, avatar_url, role, enabled)
SELECT 'user5',
       @DEMO_BCRYPT,
       'User 5',
       '13900000025',
       'user5@example.com',
       '/uploads/avatars/user5.png',
       'USER',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'user5'
       OR phone = '13900000025'
       OR email = 'user5@example.com'
);

INSERT INTO sys_user (username, password, nickname, phone, email, avatar_url, role, enabled)
SELECT 'user6',
       @DEMO_BCRYPT,
       'User 6',
       '13900000026',
       'user6@example.com',
       '/uploads/avatars/user6.png',
       'USER',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'user6'
       OR phone = '13900000026'
       OR email = 'user6@example.com'
);

INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 0.00, 'NORMAL'
FROM sys_user u
WHERE u.username = 'user2'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 0.00, 'NORMAL'
FROM sys_user u
WHERE u.username = 'user3'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 0.00, 'NORMAL'
FROM sys_user u
WHERE u.username = 'user4'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 0.00, 'NORMAL'
FROM sys_user u
WHERE u.username = 'user5'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 0.00, 'NORMAL'
FROM sys_user u
WHERE u.username = 'user6'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

-- Additional STAFF accounts
INSERT INTO sys_user (
    username,
    password,
    nickname,
    phone,
    email,
    avatar_url,
    staff_real_name,
    staff_id_card_no,
    staff_certification_url,
    staff_certification_images,
    staff_application_note,
    staff_approval_status,
    staff_approval_remark,
    role,
    enabled
)
SELECT 'staff2',
       @DEMO_BCRYPT,
       'Staff 2',
       '13900000012',
       'staff2@example.com',
       '/uploads/avatars/staff2.png',
       'Chen Yu',
       '310101199001010021',
       '/uploads/certifications/staff2-1.jpg',
       '/uploads/certifications/staff2-1.jpg,/uploads/certifications/staff2-2.jpg',
       'Skilled in green tea brewing and guest reception',
       'APPROVED',
       'Seeded attendant account',
       'STAFF',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'staff2'
       OR phone = '13900000012'
       OR email = 'staff2@example.com'
);

INSERT INTO sys_user (
    username,
    password,
    nickname,
    phone,
    email,
    avatar_url,
    staff_real_name,
    staff_id_card_no,
    staff_certification_url,
    staff_certification_images,
    staff_application_note,
    staff_approval_status,
    staff_approval_remark,
    role,
    enabled
)
SELECT 'staff3',
       @DEMO_BCRYPT,
       'Staff 3',
       '13900000013',
       'staff3@example.com',
       '/uploads/avatars/staff3.png',
       'Lin Jing',
       '310101199102140032',
       '/uploads/certifications/staff3-1.jpg',
       '/uploads/certifications/staff3-1.jpg,/uploads/certifications/staff3-2.jpg',
       'Skilled in oolong tasting and private room service',
       'APPROVED',
       'Seeded attendant account',
       'STAFF',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'staff3'
       OR phone = '13900000013'
       OR email = 'staff3@example.com'
);

INSERT INTO sys_user (
    username,
    password,
    nickname,
    phone,
    email,
    avatar_url,
    staff_real_name,
    staff_id_card_no,
    staff_certification_url,
    staff_certification_images,
    staff_application_note,
    staff_approval_status,
    staff_approval_remark,
    role,
    enabled
)
SELECT 'staff4',
       @DEMO_BCRYPT,
       'Staff 4',
       '13900000014',
       'staff4@example.com',
       '/uploads/avatars/staff4.png',
       'Zhou Lan',
       '310101199205180043',
       '/uploads/certifications/staff4-1.jpg',
       '/uploads/certifications/staff4-1.jpg,/uploads/certifications/staff4-2.jpg',
       'Skilled in floral tea service and event support',
       'APPROVED',
       'Seeded attendant account',
       'STAFF',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'staff4'
       OR phone = '13900000014'
       OR email = 'staff4@example.com'
);

INSERT INTO sys_user (
    username,
    password,
    nickname,
    phone,
    email,
    avatar_url,
    staff_real_name,
    staff_id_card_no,
    staff_certification_url,
    staff_certification_images,
    staff_application_note,
    staff_approval_status,
    staff_approval_remark,
    role,
    enabled
)
SELECT 'staff5',
       @DEMO_BCRYPT,
       'Staff 5',
       '13900000015',
       'staff5@example.com',
       '/uploads/avatars/staff5.png',
       'Tang Yue',
       '310101199308220054',
       '/uploads/certifications/staff5-1.jpg',
       '/uploads/certifications/staff5-1.jpg,/uploads/certifications/staff5-2.jpg',
       'Skilled in pu-erh guidance and member reception',
       'APPROVED',
       'Seeded attendant account',
       'STAFF',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'staff5'
       OR phone = '13900000015'
       OR email = 'staff5@example.com'
);

INSERT INTO sys_user (
    username,
    password,
    nickname,
    phone,
    email,
    avatar_url,
    staff_real_name,
    staff_id_card_no,
    staff_certification_url,
    staff_certification_images,
    staff_application_note,
    staff_approval_status,
    staff_approval_remark,
    role,
    enabled
)
SELECT 'staff6',
       @DEMO_BCRYPT,
       'Staff 6',
       '13900000016',
       'staff6@example.com',
       '/uploads/avatars/staff6.png',
       'He Qing',
       '310101199411050065',
       '/uploads/certifications/staff6-1.jpg',
       '/uploads/certifications/staff6-1.jpg,/uploads/certifications/staff6-2.jpg',
       'Skilled in white tea brewing and public room support',
       'APPROVED',
       'Seeded attendant account',
       'STAFF',
       b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'staff6'
       OR phone = '13900000016'
       OR email = 'staff6@example.com'
);
