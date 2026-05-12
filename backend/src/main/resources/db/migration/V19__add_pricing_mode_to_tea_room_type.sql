ALTER TABLE tea_room_type
    ADD COLUMN pricing_mode VARCHAR(20) NOT NULL DEFAULT 'PER_ROOM' AFTER base_price;

UPDATE tea_room_type
SET pricing_mode = CASE
    WHEN name LIKE '%套间%' THEN 'PER_ROOM'
    WHEN name LIKE '%单间%' THEN 'PER_ROOM'
    WHEN name LIKE '%公共%' THEN 'PER_PERSON'
    WHEN name LIKE '%散座%' THEN 'PER_PERSON'
    WHEN name LIKE '%大厅%' THEN 'PER_PERSON'
    ELSE 'PER_ROOM'
END;
