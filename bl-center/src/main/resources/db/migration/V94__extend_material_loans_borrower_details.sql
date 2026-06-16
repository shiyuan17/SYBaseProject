ALTER TABLE material_loans
    ADD borrower_phone VARCHAR(32);

ALTER TABLE material_loans
    ADD borrower_unit VARCHAR(200);

ALTER TABLE material_loans
    ADD deposit_amount DECIMAL(18, 2);
