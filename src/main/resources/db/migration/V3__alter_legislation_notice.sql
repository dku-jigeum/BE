ALTER TABLE legislation_notice
    DROP COLUMN notice_no,
    DROP COLUMN content,
    ADD COLUMN bill_id VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN bill_no VARCHAR(50)  NOT NULL DEFAULT '';

ALTER TABLE legislation_notice
    ADD CONSTRAINT uq_legislation_notice_bill_id UNIQUE (bill_id);

ALTER TABLE legislation_notice
    ALTER COLUMN bill_id DROP DEFAULT,
    ALTER COLUMN bill_no  DROP DEFAULT;
