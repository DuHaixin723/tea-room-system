CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    target_type VARCHAR(50) NULL,
    target_id BIGINT NULL,
    route_path VARCHAR(255) NULL,
    is_read BIT NOT NULL DEFAULT b'0',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_notification_recipient_created (recipient_user_id, created_at),
    INDEX idx_notification_recipient_read (recipient_user_id, is_read)
);
