UPDATE sys_user
SET nickname = REPLACE(nickname, '茶艺员', '茶室员')
WHERE nickname LIKE '%茶艺员%';

UPDATE activity
SET title = REPLACE(title, '茶艺员', '茶室员'),
    content = REPLACE(content, '茶艺员', '茶室员')
WHERE title LIKE '%茶艺员%' OR content LIKE '%茶艺员%';

UPDATE review
SET content = REPLACE(content, '茶艺员', '茶室员')
WHERE content LIKE '%茶艺员%';

UPDATE reservation
SET remark = REPLACE(remark, '茶艺员', '茶室员')
WHERE remark LIKE '%茶艺员%';

UPDATE equipment_report
SET title = REPLACE(title, '茶艺员', '茶室员'),
    content = REPLACE(content, '茶艺员', '茶室员')
WHERE title LIKE '%茶艺员%' OR content LIKE '%茶艺员%';

UPDATE consultation_message
SET content = REPLACE(content, '茶艺员', '茶室员')
WHERE content LIKE '%茶艺员%';

UPDATE staff_complaint
SET content = REPLACE(content, '茶艺员', '茶室员')
WHERE content LIKE '%茶艺员%';

UPDATE notification
SET title = REPLACE(title, '茶艺员', '茶室员'),
    content = REPLACE(content, '茶艺员', '茶室员')
WHERE title LIKE '%茶艺员%' OR content LIKE '%茶艺员%';

UPDATE system_config
SET config_value = REPLACE(config_value, '茶艺员', '茶室员'),
    description = REPLACE(description, '茶艺员', '茶室员')
WHERE config_value LIKE '%茶艺员%' OR description LIKE '%茶艺员%';
