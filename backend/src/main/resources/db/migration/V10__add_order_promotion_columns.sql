SET NAMES utf8mb4;

ALTER TABLE `orders` ADD COLUMN `promotion_id` BIGINT COMMENT '应用的促销活动ID' AFTER `user_coupon_id`;
ALTER TABLE `orders` ADD COLUMN `promotion_discount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '促销优惠金额' AFTER `promotion_id`;
