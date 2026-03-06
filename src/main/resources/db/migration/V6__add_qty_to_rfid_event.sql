-- Add qty column to rfid_event table to track quantities for validation
ALTER TABLE rfid_event
ADD COLUMN qty INT DEFAULT 1;

-- Update existing records to have qty = 1 (assuming each event represented 1 unit)
UPDATE rfid_event SET qty = 1 WHERE qty IS NULL;

-- Make qty NOT NULL
ALTER TABLE rfid_event
ALTER COLUMN qty SET NOT NULL;
