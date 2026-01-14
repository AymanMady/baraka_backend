-- =====================================================
-- Remove country column from shops table
-- =====================================================

-- Drop the index on country column first
DROP INDEX IF EXISTS idx_shops_country;

-- Drop the country column
ALTER TABLE shops DROP COLUMN IF EXISTS country;
