ALTER TABLE consultation_message
    ADD COLUMN notify_admin BIT NOT NULL DEFAULT b'0' AFTER sender_id;
