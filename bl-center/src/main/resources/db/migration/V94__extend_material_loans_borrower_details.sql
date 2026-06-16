ALTER TABLE material_loans
    ADD COLUMN IF NOT EXISTS borrower_phone VARCHAR(32);

ALTER TABLE material_loans
    ADD COLUMN IF NOT EXISTS borrower_unit VARCHAR(200);

ALTER TABLE material_loans
    ADD COLUMN IF NOT EXISTS deposit_amount DECIMAL(18, 2);
