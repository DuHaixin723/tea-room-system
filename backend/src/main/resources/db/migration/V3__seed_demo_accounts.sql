-- Demo accounts for local development / presentation.
-- Notes:
-- 1) Uses BCrypt hashes because the backend uses BCryptPasswordEncoder.
-- 2) Avoids duplicate inserts by checking username.
-- 3) Passwords for all demo accounts are: 123456

-- BCrypt hash for "123456" (generated locally via BCryptPasswordEncoder)
-- HASH = $2a$10$3F0M4TngaCFogHFb5hPn8O5cXUvvf8B56Hv/FGs8xzEPNo5xKV7Zm
SET @DEMO_BCRYPT := '$2a$10$3F0M4TngaCFogHFb5hPn8O5cXUvvf8B56Hv/FGs8xzEPNo5xKV7Zm';

-- ADMIN (admin portal)
INSERT INTO sys_user (username, password, nickname, phone, email, role, enabled)
SELECT 'admin',
       @DEMO_BCRYPT,
       'Admin',
       '13900000001',
       'admin@example.com',
       'ADMIN',
       b'1'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

-- STAFF (staff portal)
INSERT INTO sys_user (username, password, nickname, phone, email, role, enabled)
SELECT 'staff1',
       @DEMO_BCRYPT,
       'Staff 1',
       '13900000002',
       'staff1@example.com',
       'STAFF',
       b'1'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff1');

-- USER (user portal)
INSERT INTO sys_user (username, password, nickname, phone, email, role, enabled)
SELECT 'user1',
       @DEMO_BCRYPT,
       'User 1',
       '13900000003',
       'user1@example.com',
       'USER',
       b'1'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'user1');

-- Member account for USER role
INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 0.00, 'NORMAL'
FROM sys_user u
WHERE u.username = 'user1'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

