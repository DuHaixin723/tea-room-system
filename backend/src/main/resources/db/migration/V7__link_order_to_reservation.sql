ALTER TABLE product_order
    ADD COLUMN reservation_id BIGINT NULL;

ALTER TABLE product_order
    ADD CONSTRAINT fk_product_order_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservation (id);

CREATE INDEX idx_product_order_reservation_id ON product_order (reservation_id);
