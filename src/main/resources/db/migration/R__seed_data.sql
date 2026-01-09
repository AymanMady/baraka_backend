-- =====================================================
-- Baraka Backend - Seed Data (Repeatable Migration)
-- This migration will be re-executed whenever its content changes
-- =====================================================

-- BCrypt hash for "1234" (strength 10)
-- Insert default admin user (idempotent: ON CONFLICT DO NOTHING)
INSERT INTO users (id, full_name, phone, email, password_hash, role, is_active, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Baraka Admin',
    '20123456',
    'admin@baraka.app',
    '$2b$10$rWF/3luhXo8HvwHtjIImnukVULidzaN3PTYDMUWomFaLQQzpxTo9m',
    'ADMIN',
    true,
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- BCrypt hash for "1234" (strength 10)
-- Insert a sample merchant user (for testing)
INSERT INTO users (id, full_name, phone, email, password_hash, role, is_active, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'Merchant Demo',
    '30123456',
    'merchant@baraka.app',
    '$2b$10$rWF/3luhXo8HvwHtjIImnukVULidzaN3PTYDMUWomFaLQQzpxTo9m',
    'MERCHANT',
    true,
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- BCrypt hash for "1234" (strength 10)
-- Insert a sample customer user (for testing)
INSERT INTO users (id, full_name, phone, email, password_hash, role, is_active, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'Customer Demo',
    '40123456',
    'customer@baraka.app',
    '$2b$10$rWF/3luhXo8HvwHtjIImnukVULidzaN3PTYDMUWomFaLQQzpxTo9m',
    'CUSTOMER',
    true,
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- Insert a sample shop for the merchant (idempotent)
INSERT INTO shops (id, name, description, phone, address, city, country, latitude, longitude, status, created_by, created_at)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'Boulangerie Demo',
    'Une boulangerie artisanale proposant des paniers surprise de viennoiseries et pains du jour.',
    '50123456',
    '123 Rue de la Paix',
    'Cotonou',
    'Benin',
    6.3703,
    2.3912,
    'ACTIVE',
    'a0000000-0000-0000-0000-000000000002',
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- Insert a sample basket for the shop (idempotent)
INSERT INTO baskets (id, shop_id, title, description, price_original, price_discount, currency, quantity_total, quantity_left, pickup_start, pickup_end, status, created_at)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'Panier Viennoiseries Surprise',
    'Un assortiment de croissants, pains au chocolat et autres délices de la journée.',
    5000.00,
    2500.00,
    'MRU',
    10,
    10,
    NOW() + INTERVAL '1 hour',
    NOW() + INTERVAL '6 hours',
    'PUBLISHED',
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- Welcome notification for admin (idempotent)
INSERT INTO notifications (id, user_id, title, body, type, is_read, created_at)
VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'Bienvenue sur Baraka!',
    'Votre compte administrateur a été créé avec succès. Vous pouvez maintenant gérer la plateforme.',
    'SYSTEM',
    false,
    NOW()
)
ON CONFLICT (id) DO NOTHING;

