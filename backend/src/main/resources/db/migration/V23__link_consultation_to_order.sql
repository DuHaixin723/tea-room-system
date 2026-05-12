ALTER TABLE consultation_session
    ADD COLUMN order_id BIGINT NULL AFTER user_id;

ALTER TABLE consultation_session
    ADD CONSTRAINT fk_consultation_session_order
        FOREIGN KEY (order_id) REFERENCES product_order (id);

CREATE INDEX idx_consultation_session_order_id ON consultation_session (order_id);
