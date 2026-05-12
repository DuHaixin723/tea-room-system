ALTER TABLE sys_user
    ADD COLUMN staff_certification_images TEXT NULL AFTER staff_certification_url;

UPDATE sys_user
SET staff_certification_images = staff_certification_url
WHERE staff_certification_url IS NOT NULL
  AND staff_certification_url <> ''
  AND (staff_certification_images IS NULL OR staff_certification_images = '');
