-- =====================================================
-- BASKET IMAGES TABLE
-- =====================================================

CREATE TABLE basket_images (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    basket_id       UUID NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Foreign keys
    CONSTRAINT fk_basket_images_basket FOREIGN KEY (basket_id) REFERENCES baskets(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT chk_basket_images_display_order_positive CHECK (display_order >= 0),
    CONSTRAINT chk_basket_images_image_url_not_empty CHECK (LENGTH(TRIM(image_url)) > 0)
);

-- Indexes
CREATE INDEX idx_basket_images_basket_id ON basket_images(basket_id);
CREATE INDEX idx_basket_images_basket_display_order ON basket_images(basket_id, display_order);

COMMENT ON TABLE basket_images IS 'Images associated with baskets';
COMMENT ON COLUMN basket_images.display_order IS 'Order of image display (0 = first)';
