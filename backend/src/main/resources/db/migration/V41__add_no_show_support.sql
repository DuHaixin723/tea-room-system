ALTER TABLE reservation
    ADD COLUMN no_show_reason VARCHAR(255) NULL,
    ADD COLUMN no_show_order_no VARCHAR(64) NULL;
