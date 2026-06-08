SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `promotions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `rule_type` TINYINT NOT NULL COMMENT '规则类型: 1-满减, 2-满折',
    `threshold_amount` DECIMAL(10, 2) NOT NULL COMMENT '门槛金额',
    `discount_value` DECIMAL(10, 2) NOT NULL COMMENT '优惠值(满减为金额, 满折为比率如0.8表示8折)',
    `start_time` DATETIME NOT NULL COMMENT '生效时间',
    `end_time` DATETIME NOT NULL COMMENT '失效时间',
    `scope_type` TINYINT NOT NULL DEFAULT 0 COMMENT '适用范围: 0-全部, 1-指定品类, 2-指定商品',
    `scope_ids` VARCHAR(500) COMMENT '适用范围ID列表(JSON数组)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序(值小优先)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB COMMENT='促销活动表';

INSERT INTO `promotions` (`name`, `rule_type`, `threshold_amount`, `discount_value`, `start_time`, `end_time`, `scope_type`, `scope_ids`, `status`, `sort`) VALUES
('满30减5', 1, 30.00, 5.00, '2025-01-01 00:00:00', '2026-12-31 23:59:59', 0, NULL, 1, 1),
('满50减10', 1, 50.00, 10.00, '2025-01-01 00:00:00', '2026-12-31 23:59:59', 0, NULL, 1, 2),
('满80打8折', 2, 80.00, 0.80, '2025-01-01 00:00:00', '2026-12-31 23:59:59', 0, NULL, 1, 3);
