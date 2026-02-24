-- Add unit_price column to product table
ALTER TABLE product
ADD COLUMN IF NOT EXISTS unit_price NUMERIC(10, 2) DEFAULT 0;

-- Update existing NULL values to 0
UPDATE product SET unit_price = 0 WHERE unit_price IS NULL;

-- Create index for potential filtering by price
CREATE INDEX IF NOT EXISTS idx_product_unit_price ON product(unit_price);
