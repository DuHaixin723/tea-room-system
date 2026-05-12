CREATE TABLE staff_complaint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tea_room_id BIGINT NOT NULL,
    staff_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_complaint_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_staff_complaint_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id),
    CONSTRAINT fk_staff_complaint_staff FOREIGN KEY (staff_user_id) REFERENCES sys_user (id)
);

