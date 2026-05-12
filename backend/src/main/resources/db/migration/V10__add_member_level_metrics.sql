ALTER TABLE member_account
    ADD COLUMN cumulative_recharge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN cumulative_spend DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
