-- Migration: Convert order status from integer to enum name strings
-- Old mapping: 0=待支付, 1=制作中, 2=配送中, 3=已取消, 4=已送达, 5=已评价
-- New mapping: PENDING_PAYMENT, PAID, PREPARING, DELIVERING, CANCELLED, COMPLETED, REVIEWED

ALTER TABLE orders MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT';

UPDATE orders SET status = 'PENDING_PAYMENT' WHERE status = '0';
UPDATE orders SET status = 'PREPARING' WHERE status = '1';
UPDATE orders SET status = 'DELIVERING' WHERE status = '2';
UPDATE orders SET status = 'CANCELLED' WHERE status = '3';
UPDATE orders SET status = 'COMPLETED' WHERE status = '4';
UPDATE orders SET status = 'REVIEWED' WHERE status = '5';
