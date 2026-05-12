ALTER TABLE tea_room_type
    ADD COLUMN min_capacity INT NOT NULL DEFAULT 1 AFTER base_price,
    ADD COLUMN max_capacity INT NOT NULL DEFAULT 99 AFTER min_capacity;

UPDATE tea_room_type
SET min_capacity = 2,
    max_capacity = 4
WHERE base_price = 88.00;

UPDATE tea_room_type
SET min_capacity = 6,
    max_capacity = 10
WHERE base_price = 168.00;

UPDATE tea_room_type
SET min_capacity = 4,
    max_capacity = 8
WHERE base_price = 128.00;
