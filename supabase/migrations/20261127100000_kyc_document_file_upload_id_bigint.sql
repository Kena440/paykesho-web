-- Align KYC document file-upload reference with app.file_uploads.id (BIGINT).
-- The JPA entity stores this reference as Long; the original journey migration
-- created it as INTEGER. This is a widening conversion and preserves all values.

DO $$
DECLARE current_type text;
BEGIN
  SELECT data_type INTO current_type
  FROM information_schema.columns
  WHERE table_schema = 'app'
    AND table_name = 'kyc_documents'
    AND column_name = 'file_upload_id';

  IF current_type IS NULL THEN
    RAISE EXCEPTION 'app.kyc_documents.file_upload_id is missing';
  END IF;

  IF current_type <> 'bigint' THEN
    ALTER TABLE app.kyc_documents
      DROP CONSTRAINT IF EXISTS kyc_documents_file_upload_id_fkey;

    ALTER TABLE app.kyc_documents
      ALTER COLUMN file_upload_id TYPE bigint
      USING file_upload_id::bigint;

    ALTER TABLE app.kyc_documents
      ADD CONSTRAINT kyc_documents_file_upload_id_fkey
      FOREIGN KEY (file_upload_id) REFERENCES app.file_uploads(id);
  END IF;
END $$;
