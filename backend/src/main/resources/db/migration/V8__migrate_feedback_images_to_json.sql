UPDATE feedbacks
SET images = '[]'
WHERE images IS NULL OR TRIM(images) = '';

UPDATE feedbacks
SET images = CONCAT('["', REPLACE(TRIM(images), ',', '","'), '"]')
WHERE images IS NOT NULL
  AND TRIM(images) <> ''
  AND LEFT(TRIM(images), 1) <> '[';
