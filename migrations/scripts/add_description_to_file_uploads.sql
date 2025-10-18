-- =====================================================
-- Add description column to file_uploads table
-- =====================================================
-- This migration adds the description column that was
-- added to the FileUpload entity for better file tracking
-- =====================================================

USE paykesho_db;

-- Check if column exists before adding
SET @dbname = DATABASE();
SET @tablename = 'file_uploads';
SET @columnname = 'description';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT ''Column already exists'' AS message;',
  'ALTER TABLE file_uploads ADD COLUMN description VARCHAR(500) NULL;'
));

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Verify the column was added
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    CHARACTER_MAXIMUM_LENGTH, 
    IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'paykesho_db' 
  AND TABLE_NAME = 'file_uploads' 
  AND COLUMN_NAME = 'description';

SELECT 'Migration complete: description column added to file_uploads table' AS status;

