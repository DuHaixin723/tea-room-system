ALTER TABLE sys_user
    ADD COLUMN staff_real_name VARCHAR(50) NULL AFTER avatar_url,
    ADD COLUMN staff_id_card_no VARCHAR(30) NULL AFTER staff_real_name,
    ADD COLUMN staff_certification_url VARCHAR(255) NULL AFTER staff_id_card_no,
    ADD COLUMN staff_application_note VARCHAR(500) NULL AFTER staff_certification_url,
    ADD COLUMN staff_approval_status VARCHAR(20) NULL AFTER staff_application_note,
    ADD COLUMN staff_approval_remark VARCHAR(255) NULL AFTER staff_approval_status;

UPDATE sys_user
SET staff_approval_status = 'APPROVED'
WHERE role = 'STAFF'
  AND staff_approval_status IS NULL;
