ALTER TABLE consultation_message
    ADD COLUMN notify_staff BIT NOT NULL DEFAULT b'0' AFTER notify_admin;
