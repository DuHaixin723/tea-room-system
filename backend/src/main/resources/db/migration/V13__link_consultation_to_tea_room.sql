ALTER TABLE consultation_session
    ADD COLUMN tea_room_id BIGINT NULL AFTER user_id;

ALTER TABLE consultation_session
    ADD CONSTRAINT fk_consultation_session_room
        FOREIGN KEY (tea_room_id) REFERENCES tea_room (id);
