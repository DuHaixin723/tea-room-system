CREATE TABLE member_recharge_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    operator_user_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_recharge_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_member_recharge_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
);
