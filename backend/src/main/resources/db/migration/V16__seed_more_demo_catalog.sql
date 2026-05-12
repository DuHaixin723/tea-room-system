-- More demo data for tea rooms, teas, users, staff, and activities.

SET @DEMO_BCRYPT := '$2a$10$3F0M4TngaCFogHFb5hPn8O5cXUvvf8B56Hv/FGs8xzEPNo5xKV7Zm';

-- Additional staff accounts
INSERT INTO sys_user (username, password, nickname, phone, email, role, enabled)
SELECT 'staff2', @DEMO_BCRYPT, '林沐', '13900000012', 'staff2@example.com', 'STAFF', b'1'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff2');

INSERT INTO sys_user (username, password, nickname, phone, email, role, enabled)
SELECT 'staff3', @DEMO_BCRYPT, '许砚', '13900000013', 'staff3@example.com', 'STAFF', b'1'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff3');

-- Additional user accounts
INSERT INTO sys_user (username, password, nickname, phone, email, role, enabled)
SELECT 'user2', @DEMO_BCRYPT, '苏禾', '13900000022', 'user2@example.com', 'USER', b'1'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'user2');

INSERT INTO sys_user (username, password, nickname, phone, email, role, enabled)
SELECT 'user3', @DEMO_BCRYPT, '周宁', '13900000023', 'user3@example.com', 'USER', b'1'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'user3');

-- Member accounts for new users
INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 300.00, 'SILVER'
FROM sys_user u
WHERE u.username = 'user2'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

INSERT INTO member_account (user_id, balance, member_level)
SELECT u.id, 680.00, 'GOLD'
FROM sys_user u
WHERE u.username = 'user3'
  AND NOT EXISTS (SELECT 1 FROM member_account ma WHERE ma.user_id = u.id);

SET @STAFF2_ID := (SELECT id FROM sys_user WHERE username = 'staff2' LIMIT 1);
SET @STAFF3_ID := (SELECT id FROM sys_user WHERE username = 'staff3' LIMIT 1);
SET @USER2_ID := (SELECT id FROM sys_user WHERE username = 'user2' LIMIT 1);
SET @USER3_ID := (SELECT id FROM sys_user WHERE username = 'user3' LIMIT 1);

-- More tea room types
INSERT INTO tea_room_type (name, description, base_price, min_capacity, max_capacity)
SELECT '庭院茶席', '适合好友小聚与慢饮体验，强调景观与氛围。', 198.00, 4, 6
WHERE NOT EXISTS (SELECT 1 FROM tea_room_type WHERE name = '庭院茶席');

INSERT INTO tea_room_type (name, description, base_price, min_capacity, max_capacity)
SELECT '禅意单间', '适合安静独处、阅读和轻交流。', 108.00, 1, 2
WHERE NOT EXISTS (SELECT 1 FROM tea_room_type WHERE name = '禅意单间');

INSERT INTO tea_room_type (name, description, base_price, min_capacity, max_capacity)
SELECT '雅聚套间', '适合多人茶会、分享会和小型沙龙。', 238.00, 6, 12
WHERE NOT EXISTS (SELECT 1 FROM tea_room_type WHERE name = '雅聚套间');

SET @COURTYARD_TYPE_ID := (SELECT id FROM tea_room_type WHERE name = '庭院茶席' LIMIT 1);
SET @ZEN_TYPE_ID := (SELECT id FROM tea_room_type WHERE name = '禅意单间' LIMIT 1);
SET @SALON_TYPE_ID := (SELECT id FROM tea_room_type WHERE name = '雅聚套间' LIMIT 1);

-- More tea rooms
INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, description)
SELECT @COURTYARD_TYPE_ID, @STAFF2_ID, '茶室·竹影 03', 6, b'1', '上海徐汇', '临庭院而设，适合下午茶与好友相聚。'
WHERE NOT EXISTS (SELECT 1 FROM tea_room WHERE name = '茶室·竹影 03');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, description)
SELECT @ZEN_TYPE_ID, @STAFF2_ID, '茶室·静山 04', 2, b'1', '上海长宁', '安静私密，适合独饮、阅读和短时会面。'
WHERE NOT EXISTS (SELECT 1 FROM tea_room WHERE name = '茶室·静山 04');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, description)
SELECT @SALON_TYPE_ID, @STAFF3_ID, '茶室·听雨 05', 10, b'1', '上海浦东', '适合主题茶会、品牌分享和团体接待。'
WHERE NOT EXISTS (SELECT 1 FROM tea_room WHERE name = '茶室·听雨 05');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, description)
SELECT @COURTYARD_TYPE_ID, @STAFF3_ID, '茶室·望月 06', 4, b'1', '上海黄浦', '露台景观位，夜场氛围较好。'
WHERE NOT EXISTS (SELECT 1 FROM tea_room WHERE name = '茶室·望月 06');

-- More teas
INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '凤凰单丛', '乌龙茶', 88.00, 50, '香气高扬，适合喜欢层次感与花果香的客人。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '凤凰单丛');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '白毫银针', '白茶', 96.00, 40, '毫香清雅，茶汤柔和，适合轻口味饮茶者。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '白毫银针');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '正山小种', '红茶', 52.00, 90, '甜香明显，口感温润，适合大众口味。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '正山小种');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '陈年寿眉', '白茶', 68.00, 70, '适合久泡慢饮，滋味醇和。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '陈年寿眉');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '武夷肉桂', '乌龙茶', 76.00, 65, '岩韵鲜明，适合搭配茶点。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '武夷肉桂');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '熟普散茶', '黑茶', 45.00, 110, '口感顺滑，适合日常口粮。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '熟普散茶');

-- More activities
INSERT INTO activity (title, content, start_time, end_time, capacity, status)
SELECT '周三新手识茶夜', '面向新用户的入门活动，讲解绿茶、白茶、乌龙茶的基础区别。',
       DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 19 HOUR,
       DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 21 HOUR,
       24,
       'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '周三新手识茶夜');

INSERT INTO activity (title, content, start_time, end_time, capacity, status)
SELECT '庭院乌龙品鉴会', '在庭院茶席体验凤凰单丛与武夷肉桂的风味对比。',
       DATE_ADD(CURDATE(), INTERVAL 6 DAY) + INTERVAL 15 HOUR,
       DATE_ADD(CURDATE(), INTERVAL 6 DAY) + INTERVAL 17 HOUR,
       16,
       'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '庭院乌龙品鉴会');

INSERT INTO activity (title, content, start_time, end_time, capacity, status)
SELECT '茶艺员服务礼仪训练', '内部活动，面向茶艺员统一服务动作与接待话术。',
       DATE_ADD(CURDATE(), INTERVAL 8 DAY) + INTERVAL 10 HOUR,
       DATE_ADD(CURDATE(), INTERVAL 8 DAY) + INTERVAL 12 HOUR,
       12,
       'PENDING_REVIEW'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '茶艺员服务礼仪训练');

SET @ACTIVITY_A := (SELECT id FROM activity WHERE title = '周三新手识茶夜' LIMIT 1);
SET @ACTIVITY_B := (SELECT id FROM activity WHERE title = '庭院乌龙品鉴会' LIMIT 1);

-- Sample registrations
INSERT INTO activity_registration (activity_id, user_id, cancelled)
SELECT @ACTIVITY_A, @USER2_ID, b'0'
WHERE @ACTIVITY_A IS NOT NULL
  AND @USER2_ID IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM activity_registration
    WHERE activity_id = @ACTIVITY_A AND user_id = @USER2_ID
  );

INSERT INTO activity_registration (activity_id, user_id, cancelled)
SELECT @ACTIVITY_B, @USER3_ID, b'0'
WHERE @ACTIVITY_B IS NOT NULL
  AND @USER3_ID IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM activity_registration
    WHERE activity_id = @ACTIVITY_B AND user_id = @USER3_ID
  );
