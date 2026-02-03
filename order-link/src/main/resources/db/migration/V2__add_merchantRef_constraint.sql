-- V2__add_merchantRef_constraint.sql
-- set merchant_ref to NOT NULL if it isn't already
ALTER TABLE order_request_entries
    ALTER COLUMN merchant_ref SET NOT NULL;

ALTER TABLE order_request_entries
    ADD CONSTRAINT uq_order_request_entries_merchant_ref UNIQUE (merchant_ref);