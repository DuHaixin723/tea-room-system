INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    1,
    (SELECT id FROM sys_user WHERE username = 'staff1' LIMIT 1),
    'Orchid Court 08',
    4,
    b'1',
    'Putuo, Shanghai',
    NULL,
    '09:00:00',
    '22:00:00',
    'Private room suited for quiet tea tasting and small conversations'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 1)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff1')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Orchid Court 08');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    2,
    (SELECT id FROM sys_user WHERE username = 'staff1' LIMIT 1),
    'Cedar Whisper 09',
    8,
    b'1',
    'Minhang, Shanghai',
    NULL,
    '09:00:00',
    '22:00:00',
    'Business tea room with space for meetings and reception'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 2)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff1')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Cedar Whisper 09');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    3,
    (SELECT id FROM sys_user WHERE username = 'staff2' LIMIT 1),
    'Plum Blossom 10',
    6,
    b'1',
    'Hongkou, Shanghai',
    NULL,
    '09:30:00',
    '21:30:00',
    'Comfortable family tea room for relaxed gatherings'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 3)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff2')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Plum Blossom 10');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    4,
    (SELECT id FROM sys_user WHERE username = 'staff2' LIMIT 1),
    'Stone Spring 11',
    6,
    b'1',
    'Yangpu, Shanghai',
    NULL,
    '10:00:00',
    '22:00:00',
    'Courtyard-style tea seating with an open and airy atmosphere'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 4)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff2')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Stone Spring 11');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    5,
    (SELECT id FROM sys_user WHERE username = 'staff2' LIMIT 1),
    'Dew Terrace 12',
    2,
    b'1',
    'Baoshan, Shanghai',
    NULL,
    '09:00:00',
    '21:00:00',
    'Single-room tea space designed for reading and solo relaxation'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 5)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff2')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Dew Terrace 12');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    6,
    (SELECT id FROM sys_user WHERE username = 'staff3' LIMIT 1),
    'Ink Bamboo 13',
    10,
    b'1',
    'Songjiang, Shanghai',
    NULL,
    '09:00:00',
    '22:30:00',
    'Salon suite suitable for themed gatherings and tea sharing events'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 6)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff3')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Ink Bamboo 13');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    1,
    (SELECT id FROM sys_user WHERE username = 'staff3' LIMIT 1),
    'Hearth Pine 14',
    4,
    b'1',
    'Jiading, Shanghai',
    NULL,
    '09:30:00',
    '22:00:00',
    'Warm private tea room for close friends and short appointments'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 1)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff3')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Hearth Pine 14');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    2,
    (SELECT id FROM sys_user WHERE username = 'staff1' LIMIT 1),
    'Clear Brook 15',
    10,
    b'1',
    'Qingpu, Shanghai',
    NULL,
    '08:30:00',
    '21:30:00',
    'Spacious business room for tea reception and formal discussion'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 2)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff1')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Clear Brook 15');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    4,
    (SELECT id FROM sys_user WHERE username = 'staff2' LIMIT 1),
    'Hidden Osmanthus 16',
    4,
    b'1',
    'Fengxian, Shanghai',
    NULL,
    '10:00:00',
    '21:30:00',
    'Outdoor-inspired courtyard seating with a calm evening ambiance'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 4)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff2')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Hidden Osmanthus 16');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, image_url, business_start_time, business_end_time, description)
SELECT
    7,
    (SELECT id FROM sys_user WHERE username = 'staff3' LIMIT 1),
    'Dawn Mist 17',
    18,
    b'1',
    'Jinshan, Shanghai',
    NULL,
    '09:00:00',
    '22:00:00',
    'Public tea room for walk-in guests and small group experiences'
WHERE EXISTS (SELECT 1 FROM tea_room_type WHERE id = 7)
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'staff3')
  AND NOT EXISTS (SELECT 1 FROM tea_room WHERE name = 'Dawn Mist 17');
