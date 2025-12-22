-- Fix photo_path field consistency for merchants table
-- This script ensures the photo_path field is properly populated and the redundant camelCase column is removed

-- Note: The camelCase photoPath column has been manually removed from the database
-- This script ensures data consistency for the photo_path field

-- Update any NULL photo_path values where we have file references
-- This handles cases where merchants have remote_file_id but no photo_path set
UPDATE merchants m
SET photo_path = (
    SELECT CONCAT('https://files.paykesho.com/uploads/', 
                  CASE 
                      WHEN fu.file_type = 'logo' AND fu.file_path LIKE '%/logos/%' THEN 'logos/'
                      WHEN fu.file_type = 'logo' THEN 'logo/'
                      ELSE LOWER(fu.file_type) || '/'
                  END,
                  SUBSTRING_INDEX(fu.file_path, '/', -1))
    FROM file_uploads fu 
    WHERE fu.id = m.remote_file_id
)
WHERE m.photo_path IS NULL 
AND m.remote_file_id IS NOT NULL
AND EXISTS (
    SELECT 1 FROM file_uploads fu WHERE fu.id = m.remote_file_id
);

-- Verify the changes
SELECT id, name, photo_path, remote_file_id 
FROM merchants 
WHERE photo_path IS NOT NULL 
ORDER BY id;
