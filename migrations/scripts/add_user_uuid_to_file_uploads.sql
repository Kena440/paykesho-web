-- Migration script to add user_uuid column to file_uploads table
-- This enables UUID-based file upload functionality
-- Works with any database name (uses DATABASE() function)

-- Add user_uuid column to file_uploads table if it doesn't exist
SET @exist := (SELECT COUNT(*) 
               FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'file_uploads' 
               AND COLUMN_NAME = 'user_uuid');

SET @sqlstmt := IF(@exist > 0, 
    'SELECT "Column user_uuid already exists in file_uploads table" AS Info',
    'ALTER TABLE file_uploads ADD COLUMN user_uuid VARCHAR(36) AFTER user_id'
);

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Make user_id nullable since we now support UUID-based uploads
SET @exist_not_null := (SELECT COUNT(*) 
                        FROM INFORMATION_SCHEMA.COLUMNS 
                        WHERE TABLE_SCHEMA = DATABASE()
                        AND TABLE_NAME = 'file_uploads' 
                        AND COLUMN_NAME = 'user_id' 
                        AND IS_NULLABLE = 'NO');

SET @sqlstmt_nullable := IF(@exist_not_null > 0,
    'ALTER TABLE file_uploads MODIFY COLUMN user_id INT NULL',
    'SELECT "Column user_id is already nullable" AS Info'
);

PREPARE stmt_nullable FROM @sqlstmt_nullable;
EXECUTE stmt_nullable;
DEALLOCATE PREPARE stmt_nullable;

-- Add index on user_uuid for faster lookups
SET @exist_index := (SELECT COUNT(*) 
                     FROM INFORMATION_SCHEMA.STATISTICS 
                     WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'file_uploads' 
                     AND INDEX_NAME = 'idx_file_uploads_user_uuid');

SET @sqlstmt_index := IF(@exist_index > 0,
    'SELECT "Index idx_file_uploads_user_uuid already exists" AS Info',
    'CREATE INDEX idx_file_uploads_user_uuid ON file_uploads(user_uuid)'
);

PREPARE stmt_index FROM @sqlstmt_index;
EXECUTE stmt_index;
DEALLOCATE PREPARE stmt_index;

-- Verify the changes
SELECT 'File uploads UUID migration completed successfully!' AS Status;

SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'file_uploads'
AND COLUMN_NAME IN ('user_id', 'user_uuid')
ORDER BY ORDINAL_POSITION;

