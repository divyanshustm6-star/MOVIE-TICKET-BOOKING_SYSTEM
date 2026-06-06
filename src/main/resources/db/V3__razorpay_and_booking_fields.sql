-- Migration: add razorpay columns to payments and add booking payment fields

ALTER TABLE payments
    ADD COLUMN razorpay_order_id VARCHAR(120),
    ADD COLUMN razorpay_payment_id VARCHAR(120),
    ADD COLUMN razorpay_signature VARCHAR(255);

ALTER TABLE bookings
    ADD COLUMN payment_status VARCHAR(40) DEFAULT 'INITIATED',
    ADD COLUMN ticket_path VARCHAR(500),
    ADD COLUMN paid_at TIMESTAMP NULL;

-- sync existing show_seats if needed (no-op)
