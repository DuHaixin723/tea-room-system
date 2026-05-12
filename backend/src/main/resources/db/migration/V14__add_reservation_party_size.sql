ALTER TABLE reservation
    ADD COLUMN party_size INT NOT NULL DEFAULT 1 AFTER reserved_end_time;
