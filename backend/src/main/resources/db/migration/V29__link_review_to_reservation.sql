ALTER TABLE review
    ADD COLUMN reservation_id BIGINT NULL;

ALTER TABLE review
    ADD CONSTRAINT fk_review_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservation(id);

CREATE UNIQUE INDEX uk_review_reservation_id ON review (reservation_id);
