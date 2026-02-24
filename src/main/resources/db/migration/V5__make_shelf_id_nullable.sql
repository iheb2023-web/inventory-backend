-- Make shelf_id nullable to support PRODUCT_WITHOUT_STOCK_EXIT alerts
-- which are not associated with a specific shelf

-- Drop the NOT NULL constraint on shelf_id
ALTER TABLE alert
ALTER COLUMN shelf_id DROP NOT NULL;

-- Update the foreign key constraint to handle NULLs
ALTER TABLE alert
DROP CONSTRAINT IF EXISTS alert_shelf_id_fkey;

ALTER TABLE alert
ADD CONSTRAINT alert_shelf_id_fkey FOREIGN KEY (shelf_id) REFERENCES shelf(id) ON DELETE SET NULL;
