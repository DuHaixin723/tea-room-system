INSERT INTO tea_room_type (name, description, base_price, pricing_mode, min_capacity, max_capacity)
SELECT '公共茶室', '开放式公共茶席，按到店人数预约与计费，适合散客、小聚与拼桌体验。', 58.00, 'PER_PERSON', 1, 20
WHERE NOT EXISTS (SELECT 1 FROM tea_room_type WHERE name = '公共茶室');

SET @PUBLIC_TEA_ROOM_TYPE_ID := (SELECT id FROM tea_room_type WHERE name = '公共茶室' LIMIT 1);
SET @PUBLIC_TEA_ROOM_STAFF_ID := (
    SELECT id FROM sys_user
    WHERE role = 'STAFF'
    ORDER BY id
    LIMIT 1
);

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, description, business_start_time, business_end_time)
SELECT @PUBLIC_TEA_ROOM_TYPE_ID,
       @PUBLIC_TEA_ROOM_STAFF_ID,
       '公共茶室·云水间',
       20,
       b'1',
       '上海静安',
       '开放共享茶席，按人数预约，支持散客与小团体到店体验。',
       '09:00:00',
       '22:00:00'
WHERE @PUBLIC_TEA_ROOM_TYPE_ID IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = '公共茶室·云水间');
