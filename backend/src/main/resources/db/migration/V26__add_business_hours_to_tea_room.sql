ALTER TABLE tea_room
    ADD COLUMN business_start_time TIME NOT NULL DEFAULT '09:00:00',
    ADD COLUMN business_end_time TIME NOT NULL DEFAULT '22:00:00';
