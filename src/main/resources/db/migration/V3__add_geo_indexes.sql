-- =====================================================
-- Baraka Backend - Geographic Indexes
-- Version: V3
-- =====================================================

-- Add composite index on shops for geo queries (bounding box)
CREATE INDEX IF NOT EXISTS idx_shops_geo_bbox 
    ON shops(status, latitude, longitude) 
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Add separate indexes for latitude and longitude range queries
CREATE INDEX IF NOT EXISTS idx_shops_latitude ON shops(latitude) WHERE latitude IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_shops_longitude ON shops(longitude) WHERE longitude IS NOT NULL;

-- Add index on baskets for available baskets queries
CREATE INDEX IF NOT EXISTS idx_baskets_available_geo 
    ON baskets(status, quantity_left, pickup_end) 
    WHERE status = 'PUBLISHED' AND quantity_left > 0;

