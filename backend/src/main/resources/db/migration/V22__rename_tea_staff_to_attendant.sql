UPDATE sys_user
SET nickname = REPLACE(nickname, '茶艺师', '茶艺员')
WHERE nickname LIKE '%茶艺师%';

UPDATE activity
SET title = REPLACE(title, '茶艺师', '茶艺员'),
    content = REPLACE(content, '茶艺师', '茶艺员')
WHERE title LIKE '%茶艺师%' OR content LIKE '%茶艺师%';

UPDATE review
SET content = REPLACE(content, '茶艺师', '茶艺员')
WHERE content LIKE '%茶艺师%';

UPDATE reservation
SET remark = REPLACE(remark, '茶艺师', '茶艺员')
WHERE remark LIKE '%茶艺师%';

UPDATE equipment_report
SET title = REPLACE(title, '茶艺师', '茶艺员'),
    content = REPLACE(content, '茶艺师', '茶艺员')
WHERE title LIKE '%茶艺师%' OR content LIKE '%茶艺师%';

UPDATE consultation_message
SET content = REPLACE(content, '茶艺师', '茶艺员')
WHERE content LIKE '%茶艺师%';

UPDATE staff_complaint
SET content = REPLACE(content, '茶艺师', '茶艺员')
WHERE content LIKE '%茶艺师%';

UPDATE system_config
SET config_value = REPLACE(config_value, '茶艺师', '茶艺员'),
    description = REPLACE(description, '茶艺师', '茶艺员')
WHERE config_value LIKE '%茶艺师%' OR description LIKE '%茶艺师%';
