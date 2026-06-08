-- Set Charset
SET NAMES utf8mb4;

-- Create Database
CREATE DATABASE IF NOT EXISTS milktea DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE milktea;

-- 1. 用户表
CREATE TABLE `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `phone` VARCHAR(20) COMMENT '手机号',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER, ADMIN',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户信息表';

-- 2. 商品分类表
CREATE TABLE `categories` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='商品分类表';

-- 3. 商品表
CREATE TABLE `products` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '价格',
    `image` VARCHAR(255) COMMENT '商品图片',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-下架, 1-上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB COMMENT='商品表';

-- 4. 购物车表
CREATE TABLE `cart_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `specs` VARCHAR(255) COMMENT '规格选项 (JSON 格式)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB COMMENT='购物车项表';

-- 5. 订单主表
CREATE TABLE `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_sn` VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    `discount_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '优惠金额',
    `pay_amount` DECIMAL(10, 2) NOT NULL COMMENT '实付金额',
    `user_coupon_id` BIGINT COMMENT '使用的优惠券ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-制作中, 2-配送中, 4-已送达, 5-已评价',
    `remark` VARCHAR(255) COMMENT '备注',
    `cancel_reason` VARCHAR(255) COMMENT '取消原因',
    `address_id` BIGINT COMMENT '收货地址ID',
    `address_contact_name` VARCHAR(50) COMMENT '收货联系人',
    `address_phone` VARCHAR(20) COMMENT '收货手机号',
    `address_full` VARCHAR(500) COMMENT '完整收货地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='订单主表';

-- 6. 订单详情表
CREATE TABLE `order_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `product_price` DECIMAL(10, 2) NOT NULL COMMENT '下单时价格',
    `quantity` INT NOT NULL COMMENT '数量',
    `specs` VARCHAR(255) COMMENT '规格选项',
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB COMMENT='订单详情表';

-- 7. 评价表
CREATE TABLE `feedbacks` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `rating` TINYINT NOT NULL COMMENT '评分 (1-5)',
    `content` TEXT COMMENT '评价内容',
    `images` TEXT COMMENT '评价图片URL (JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB COMMENT='评价与反馈表';

-- 初始化数据
INSERT INTO `users` (username, password, nickname, role) VALUES ('admin', '$2a$10$ByIUiNa.QMG7X1KNo8Zbe.8S0FbaqH60b2.M8.f6E9y/.Cea.vD1G', '管理员', 'ADMIN');

INSERT INTO `categories` (name, sort) VALUES ('招牌推荐', 1), ('浓醇奶茶', 2), ('清爽果茶', 3), ('芝士奶盖', 4);

INSERT INTO `products` (category_id, name, description, price, image) VALUES 
(1, '杨枝甘露', '新鲜芒果配上西米，清爽解腻', 18.00, '/images/mango.png'),
(1, '多肉葡萄', '饱满葡萄果肉，清新回甘', 22.00, '/images/grape.png'),
(2, '经典珍珠奶茶', 'Q弹珍珠，古法熬制', 12.00, '/images/pearl.png'),
(2, '波波烤奶', '焦香奶盖，口感丰富', 15.00, '/images/boba.png'),
(3, '满杯红柚', '新鲜红柚，酸甜清爽', 19.00, '/images/grapefruit.png'),
(4, '芝芝莓莓', '新鲜莓果，搭配浓郁咸香奶盖', 25.00, '/images/strawberry_cheese.png');

-- 8. 优惠券表
CREATE TABLE `coupons` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(8) NOT NULL UNIQUE COMMENT '优惠券码',
    `type` TINYINT NOT NULL COMMENT '类型: 1-满减, 2-折扣',
    `threshold` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '门槛金额',
    `value` DECIMAL(10, 2) NOT NULL COMMENT '优惠值(满减为金额, 折扣为比率如0.8表示8折)',
    `start_time` DATETIME NOT NULL COMMENT '有效期开始',
    `end_time` DATETIME NOT NULL COMMENT '有效期结束',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '总发行量',
    `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用量(含已领取)',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='优惠券表';

-- 9. 用户优惠券表
CREATE TABLE `user_coupons` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-未使用, 1-已使用',
    `order_id` BIGINT COMMENT '使用的订单ID',
    `use_time` DATETIME COMMENT '使用时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`)
) ENGINE=InnoDB COMMENT='用户优惠券表';

-- 10. 会员等级表
CREATE TABLE `member_levels` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `level` TINYINT NOT NULL DEFAULT 0 COMMENT '等级: 0-普通, 1-银卡, 2-金卡, 3-黑卡',
    `total_points` INT NOT NULL DEFAULT 0 COMMENT '累计总积分',
    `current_points` INT NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='会员等级表';

-- 11. 积分记录表
CREATE TABLE `points_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_id` BIGINT COMMENT '关联订单ID',
    `points` INT NOT NULL COMMENT '积分变动值(正数增加,负数扣减)',
    `type` TINYINT NOT NULL COMMENT '类型: 1-消费获得, 2-管理员调整, 3-订单取消扣减',
    `description` VARCHAR(255) COMMENT '描述',
    `balance` INT NOT NULL COMMENT '变动后余额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='积分记录表';

-- 12. 收货地址表
CREATE TABLE `addresses` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `province` VARCHAR(50) NOT NULL COMMENT '省',
    `city` VARCHAR(50) NOT NULL COMMENT '市',
    `district` VARCHAR(50) NOT NULL COMMENT '区',
    `detail_address` VARCHAR(200) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认: 0-否, 1-是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='收货地址表';

-- 13. 订单取消日志表
CREATE TABLE `order_cancel_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `cancel_reason` VARCHAR(255) NOT NULL COMMENT '取消原因',
    `operator` VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '操作者: SYSTEM-系统自动, USER-用户手动',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB COMMENT='订单取消日志表';

-- 14. 站内通知表
CREATE TABLE `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(1000) NOT NULL COMMENT '通知内容',
    `type` VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '类型: ORDER-订单, ACTIVITY-活动, SYSTEM-系统',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `business_id` BIGINT COMMENT '关联业务ID(如订单ID)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB COMMENT='站内通知表';

-- 15. 商品收藏表
CREATE TABLE `favorites` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB COMMENT='商品收藏表';
