-- Add product_id and product_name columns to alert table
ALTER TABLE alert
ADD COLUMN IF NOT EXISTS product_id INT,
ADD COLUMN IF NOT EXISTS product_name VARCHAR(100);

-- Update the alert_type CHECK constraint to include PRODUCT_WITHOUT_STOCK_EXIT
ALTER TABLE alert
DROP CONSTRAINT IF EXISTS alert_alert_type_check;

ALTER TABLE alert
ADD CONSTRAINT alert_alert_type_check CHECK (alert_type IN ('LOW_WEIGHT', 'PRODUCT_WITHOUT_STOCK_EXIT'));

-- Add foreign key constraint for product_id
ALTER TABLE alert
ADD CONSTRAINT fk_alert_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

-- Create index on product_id for better query performance
CREATE INDEX IF NOT EXISTS idx_alert_product ON alert(product_id);
