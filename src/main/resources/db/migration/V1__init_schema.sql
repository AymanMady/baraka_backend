-- =====================================================
-- Baraka Backend - Initial Schema
-- Version: V1
-- Database: PostgreSQL
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- ENUM TYPES (stored as VARCHAR in entities, but can use native ENUMs)
-- =====================================================

CREATE TYPE user_role AS ENUM ('CUSTOMER', 'MERCHANT', 'ADMIN');
CREATE TYPE shop_status AS ENUM ('PENDING', 'ACTIVE', 'SUSPENDED');
CREATE TYPE basket_status AS ENUM ('DRAFT', 'PUBLISHED', 'SOLD_OUT', 'EXPIRED');
CREATE TYPE order_status AS ENUM ('RESERVED', 'PICKED_UP', 'CANCELLED', 'NO_SHOW');
CREATE TYPE payment_provider AS ENUM ('STRIPE', 'MOBILE_MONEY', 'CASH');
CREATE TYPE payment_status AS ENUM ('UNPAID', 'PENDING', 'PAID', 'REFUNDED');
CREATE TYPE notification_type AS ENUM ('ORDER_CONFIRMED', 'ORDER_READY', 'ORDER_CANCELLED', 'PAYMENT_RECEIVED', 'PROMOTION', 'SYSTEM');

-- =====================================================
-- USERS TABLE
-- =====================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20) NOT NULL,
    email           VARCHAR(255),
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,

    -- Unique constraints
    CONSTRAINT uk_users_phone UNIQUE (phone),
    CONSTRAINT uk_users_email UNIQUE (email),

    -- Check constraints
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'MERCHANT', 'ADMIN')),
    CONSTRAINT chk_users_phone_not_empty CHECK (LENGTH(TRIM(phone)) >= 8),
    CONSTRAINT chk_users_full_name_not_empty CHECK (LENGTH(TRIM(full_name)) >= 2)
);

-- Indexes
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

COMMENT ON TABLE users IS 'User accounts for customers, merchants and admins';

-- =====================================================
-- SHOPS TABLE
-- =====================================================

CREATE TABLE shops (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    phone           VARCHAR(20),
    address         VARCHAR(255),
    city            VARCHAR(100),
    country         VARCHAR(100),
    latitude        NUMERIC(10, 8),
    longitude       NUMERIC(11, 8),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,

    -- Foreign keys
    CONSTRAINT fk_shops_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,

    -- Check constraints
    CONSTRAINT chk_shops_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_shops_name_not_empty CHECK (LENGTH(TRIM(name)) >= 2),
    CONSTRAINT chk_shops_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT chk_shops_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

-- Indexes
CREATE INDEX idx_shops_status ON shops(status);
CREATE INDEX idx_shops_city ON shops(city);
CREATE INDEX idx_shops_country ON shops(country);
CREATE INDEX idx_shops_created_by ON shops(created_by);
CREATE INDEX idx_shops_status_city ON shops(status, city);
CREATE INDEX idx_shops_location ON shops(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

COMMENT ON TABLE shops IS 'Merchant shops/stores';

-- =====================================================
-- BASKETS TABLE
-- =====================================================

CREATE TABLE baskets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shop_id         UUID NOT NULL,
    title           VARCHAR(150) NOT NULL,
    description     TEXT,
    price_original  NUMERIC(10, 2) NOT NULL,
    price_discount  NUMERIC(10, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'XOF',
    quantity_total  INTEGER NOT NULL,
    quantity_left   INTEGER NOT NULL,
    pickup_start    TIMESTAMPTZ NOT NULL,
    pickup_end      TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,

    -- Foreign keys
    CONSTRAINT fk_baskets_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT chk_baskets_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'SOLD_OUT', 'EXPIRED')),
    CONSTRAINT chk_baskets_price_original_positive CHECK (price_original >= 0),
    CONSTRAINT chk_baskets_price_discount_positive CHECK (price_discount >= 0),
    CONSTRAINT chk_baskets_price_discount_lte_original CHECK (price_discount <= price_original),
    CONSTRAINT chk_baskets_quantity_total_positive CHECK (quantity_total >= 1),
    CONSTRAINT chk_baskets_quantity_left_positive CHECK (quantity_left >= 0),
    CONSTRAINT chk_baskets_quantity_left_lte_total CHECK (quantity_left <= quantity_total),
    CONSTRAINT chk_baskets_pickup_end_after_start CHECK (pickup_end > pickup_start),
    CONSTRAINT chk_baskets_title_not_empty CHECK (LENGTH(TRIM(title)) >= 2)
);

-- Indexes
CREATE INDEX idx_baskets_shop_id ON baskets(shop_id);
CREATE INDEX idx_baskets_status ON baskets(status);
CREATE INDEX idx_baskets_pickup_start ON baskets(pickup_start);
CREATE INDEX idx_baskets_pickup_end ON baskets(pickup_end);
CREATE INDEX idx_baskets_status_pickup ON baskets(status, pickup_start, pickup_end);
CREATE INDEX idx_baskets_shop_status ON baskets(shop_id, status);
CREATE INDEX idx_baskets_available ON baskets(status, pickup_start, pickup_end, quantity_left) 
    WHERE status = 'PUBLISHED' AND quantity_left > 0;

COMMENT ON TABLE baskets IS 'Surprise baskets offered by shops';

-- =====================================================
-- ORDERS TABLE
-- =====================================================

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    basket_id       UUID NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(10, 2) NOT NULL,
    total_price     NUMERIC(10, 2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    pickup_code     VARCHAR(10) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,

    -- Foreign keys
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_basket FOREIGN KEY (basket_id) REFERENCES baskets(id) ON DELETE RESTRICT,

    -- Unique constraints
    CONSTRAINT uk_orders_pickup_code UNIQUE (pickup_code),

    -- Check constraints
    CONSTRAINT chk_orders_status CHECK (status IN ('RESERVED', 'PICKED_UP', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_orders_quantity_positive CHECK (quantity >= 1),
    CONSTRAINT chk_orders_unit_price_positive CHECK (unit_price >= 0),
    CONSTRAINT chk_orders_total_price_positive CHECK (total_price >= 0),
    CONSTRAINT chk_orders_total_price_matches CHECK (total_price = unit_price * quantity),
    CONSTRAINT chk_orders_pickup_code_format CHECK (LENGTH(pickup_code) >= 6)
);

-- Indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_basket_id ON orders(basket_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_orders_pickup_code ON orders(pickup_code);

COMMENT ON TABLE orders IS 'Customer orders for baskets';

-- =====================================================
-- PAYMENTS TABLE
-- =====================================================

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL,
    provider        VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,

    -- Foreign keys
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,

    -- Unique constraints
    CONSTRAINT uk_payments_order_id UNIQUE (order_id),

    -- Check constraints
    CONSTRAINT chk_payments_provider CHECK (provider IN ('STRIPE', 'MOBILE_MONEY', 'CASH')),
    CONSTRAINT chk_payments_status CHECK (status IN ('UNPAID', 'PENDING', 'PAID', 'REFUNDED')),
    CONSTRAINT chk_payments_paid_at_required CHECK (
        (status IN ('PAID', 'REFUNDED') AND paid_at IS NOT NULL) OR
        (status IN ('UNPAID', 'PENDING') AND paid_at IS NULL)
    )
);

-- Indexes
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_provider ON payments(provider);
CREATE INDEX idx_payments_paid_at ON payments(paid_at) WHERE paid_at IS NOT NULL;
CREATE INDEX idx_payments_created_at ON payments(created_at);

COMMENT ON TABLE payments IS 'Payment records for orders';

-- =====================================================
-- REVIEWS TABLE
-- =====================================================

CREATE TABLE reviews (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL,
    shop_id         UUID NOT NULL,
    user_id         UUID NOT NULL,
    rating          INTEGER NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,

    -- Foreign keys
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraints (one review per order)
    CONSTRAINT uk_reviews_order_id UNIQUE (order_id),

    -- Check constraints
    CONSTRAINT chk_reviews_rating_range CHECK (rating >= 1 AND rating <= 5)
);

-- Indexes
CREATE INDEX idx_reviews_shop_id ON reviews(shop_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
CREATE INDEX idx_reviews_shop_rating ON reviews(shop_id, rating);
CREATE INDEX idx_reviews_shop_created ON reviews(shop_id, created_at DESC);

COMMENT ON TABLE reviews IS 'Customer reviews for completed orders';

-- =====================================================
-- FAVORITES TABLE (composite primary key)
-- =====================================================

CREATE TABLE favorites (
    user_id         UUID NOT NULL,
    shop_id         UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Primary key (composite)
    PRIMARY KEY (user_id, shop_id),

    -- Foreign keys
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_shop_id ON favorites(shop_id);
CREATE INDEX idx_favorites_created_at ON favorites(created_at);

COMMENT ON TABLE favorites IS 'User favorite shops';

-- =====================================================
-- NOTIFICATIONS TABLE
-- =====================================================

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT NOT NULL,
    type            VARCHAR(30) NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Foreign keys
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT chk_notifications_type CHECK (type IN ('ORDER_CONFIRMED', 'ORDER_READY', 'ORDER_CANCELLED', 'PAYMENT_RECEIVED', 'PROMOTION', 'SYSTEM')),
    CONSTRAINT chk_notifications_title_not_empty CHECK (LENGTH(TRIM(title)) >= 1)
);

-- Indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_user_read_created ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_notifications_type ON notifications(type);

COMMENT ON TABLE notifications IS 'User notifications';

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to all tables with updated_at column
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_shops_updated_at
    BEFORE UPDATE ON shops
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_baskets_updated_at
    BEFORE UPDATE ON baskets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
