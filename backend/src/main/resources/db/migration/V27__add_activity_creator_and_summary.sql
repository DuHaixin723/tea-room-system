ALTER TABLE activity
    ADD COLUMN creator_user_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN creator_role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    ADD COLUMN summary_content TEXT NULL,
    ADD COLUMN summary_submitted_at DATETIME NULL;
