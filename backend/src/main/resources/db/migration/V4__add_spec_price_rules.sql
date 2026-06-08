ALTER TABLE products ADD COLUMN spec_price_rules TEXT COMMENT '规格加价规则(JSON)' AFTER low_stock_threshold;
ALTER TABLE cart_items ADD COLUMN unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '规格加价后单价' AFTER specs;

UPDATE products SET spec_price_rules = '{"size":{"大杯":3},"topping":{"珍珠":2,"布丁":2}}' WHERE name = '经典珍珠奶茶';
UPDATE products SET spec_price_rules = '{"size":{"大杯":3},"topping":{"珍珠":2,"布丁":2}}' WHERE name = '波波烤奶';
UPDATE products SET spec_price_rules = '{"size":{"大杯":3},"topping":{"珍珠":2}}' WHERE name = '杨枝甘露';
UPDATE products SET spec_price_rules = '{"size":{"大杯":3},"topping":{"珍珠":2}}' WHERE name = '多肉葡萄';
UPDATE products SET spec_price_rules = '{"size":{"大杯":3},"topping":{"布丁":2}}' WHERE name = '满杯红柚';
UPDATE products SET spec_price_rules = '{"size":{"大杯":3},"topping":{"布丁":2}}' WHERE name = '芝芝莓莓';

UPDATE cart_items ci JOIN products p ON ci.product_id = p.id SET ci.unit_price = p.price WHERE ci.unit_price = 0;
