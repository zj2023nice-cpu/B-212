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
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-制作中, 2-配送中, 4-已送达, 5-已评价',
    `remark` VARCHAR(255) COMMENT '备注',
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
    `rating` TINYINT NOT NULL COMMENT '评分 (1-5)',
    `content` TEXT COMMENT '评价内容',
    `images` TEXT COMMENT '评价图片URL (JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
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
