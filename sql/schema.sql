-- ============================================================
-- 商品管理系统（goods）建表脚本 + 演示种子数据
-- 数据库: MySQL 8.x / 5.7+    字符集: utf8mb4
-- 用法: mysql -u root -p < sql/schema.sql
--       或登录 MySQL 后执行: source sql/schema.sql
-- 说明: 脚本可重复执行（CREATE IF NOT EXISTS + INSERT IGNORE）
--       未使用外键约束，避免演示时删改数据被阻塞
-- ============================================================

CREATE DATABASE IF NOT EXISTS `shutiao`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `shutiao`;

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`         VARCHAR(64)  NOT NULL COMMENT '用户名',
    `email`            VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `password_hash`    VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    `avatar`           VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    `role`             VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER / ADMIN / SELLER',
    `status`           VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE / BANNED',
    `bio`              VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `website`          VARCHAR(255) DEFAULT NULL COMMENT '个人网站',
    `company_name`     VARCHAR(128) DEFAULT NULL COMMENT '卖家店铺/公司名称',
    `contact_phone`    VARCHAR(32)  DEFAULT NULL COMMENT '卖家联系电话',
    `business_license` VARCHAR(64)  DEFAULT NULL COMMENT '营业执照号',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_role` (`role`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 商品分类表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `categories` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100) NOT NULL COMMENT '分类名称',
    `slug`        VARCHAR(100) DEFAULT NULL COMMENT '分类别名',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    `parent_id`   INT          DEFAULT 0 COMMENT '父分类ID，0 为顶级',
    `order_num`   INT          DEFAULT 0 COMMENT '排序号',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_categories_slug` (`slug`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品分类表';

-- ------------------------------------------------------------
-- 3. 商品表（实体 Goods 映射到 products）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `products` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(200)  NOT NULL COMMENT '商品名称',
    `description`    TEXT          COMMENT '商品描述',
    `main_image`     VARCHAR(500)  DEFAULT NULL COMMENT '主图地址（以 /img/ 开头）',
    `image_list`     TEXT          COMMENT '图片列表（JSON 数组）',
    `price`          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `stock`          INT           NOT NULL DEFAULT 0 COMMENT '库存',
    `sales_count`    INT           NOT NULL DEFAULT 0 COMMENT '销量',
    `category_id`    BIGINT        DEFAULT NULL COMMENT '分类ID',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0 下架/定时待发布, 1 上架, 2 已取消定时',
    `seller_id`      BIGINT        DEFAULT NULL COMMENT '卖家用户ID',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `scheduled_time` DATETIME      DEFAULT NULL COMMENT '定时上架时间',
    PRIMARY KEY (`id`),
    KEY `idx_products_category` (`category_id`),
    KEY `idx_products_seller` (`seller_id`),
    KEY `idx_products_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品表';

-- ------------------------------------------------------------
-- 4. 购物车表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cart` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
    `goods_id`   BIGINT   NOT NULL COMMENT '商品ID',
    `quantity`   INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_user_goods` (`user_id`, `goods_id`),
    KEY `idx_cart_goods` (`goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '购物车表';

-- ------------------------------------------------------------
-- 5. 订单表（实体 OrderInfo）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`         VARCHAR(64)   NOT NULL COMMENT '订单号',
    `user_id`          BIGINT        NOT NULL COMMENT '下单用户ID',
    `total_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `pay_amount`       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '0待付款 1待发货 2已发货 3已完成 4已取消 5退款中',
    `receiver_name`    VARCHAR(64)   DEFAULT NULL COMMENT '收货人',
    `receiver_phone`   VARCHAR(32)   DEFAULT NULL COMMENT '收货电话',
    `receiver_address` VARCHAR(255)  DEFAULT NULL COMMENT '收货地址',
    `pay_type`         TINYINT       DEFAULT NULL COMMENT '支付方式: 1 模拟支付',
    `pay_time`         DATETIME      DEFAULT NULL COMMENT '支付时间',
    `delivery_time`    DATETIME      DEFAULT NULL COMMENT '发货时间',
    `complete_time`    DATETIME      DEFAULT NULL COMMENT '完成时间',
    `express_company`  VARCHAR(64)   DEFAULT NULL COMMENT '快递公司',
    `express_no`       VARCHAR(64)   DEFAULT NULL COMMENT '快递单号',
    `remark`           VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    KEY `idx_orders_user` (`user_id`),
    KEY `idx_orders_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单表';

-- ------------------------------------------------------------
-- 6. 订单明细表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_item` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT        NOT NULL COMMENT '订单ID',
    `goods_id`    BIGINT        NOT NULL COMMENT '商品ID',
    `goods_name`  VARCHAR(200)  DEFAULT NULL COMMENT '下单时商品名称快照',
    `goods_image` VARCHAR(500)  DEFAULT NULL COMMENT '下单时商品图片快照',
    `goods_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '下单时单价',
    `quantity`    INT           NOT NULL DEFAULT 1 COMMENT '数量',
    `subtotal`    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '小计',
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order` (`order_id`),
    KEY `idx_order_item_goods` (`goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单明细表';

-- ------------------------------------------------------------
-- 7. 收货地址表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `address` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `receiver_name`  VARCHAR(64)  NOT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(32)  NOT NULL COMMENT '收货电话',
    `province`       VARCHAR(64)  DEFAULT NULL COMMENT '省',
    `city`           VARCHAR(64)  DEFAULT NULL COMMENT '市',
    `district`       VARCHAR(64)  DEFAULT NULL COMMENT '区/县',
    `detail_address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
    `is_default`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认: 0 否, 1 是',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_address_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收货地址表';

-- ------------------------------------------------------------
-- 8. 评论表（商品评论 / 文章评论）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `comments` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `content`     TEXT         NOT NULL COMMENT '评论内容',
    `goods_id`    BIGINT       DEFAULT NULL COMMENT '商品ID（商品评论时填写）',
    `username`    VARCHAR(64)  DEFAULT NULL COMMENT '评论人用户名',
    `rating`      TINYINT      DEFAULT NULL COMMENT '评分 1-5',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '评论用户ID',
    `target_type` TINYINT      DEFAULT 1 COMMENT '评论对象: 1 商品, 2 文章',
    `target_id`   BIGINT       DEFAULT NULL COMMENT '评论对象ID',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT '父评论ID（回复时填写）',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0 待审核, 1 已通过, 2 已拒绝',
    `ip_address`  VARCHAR(64)  DEFAULT NULL COMMENT '评论IP',
    `user_agent`  VARCHAR(255) DEFAULT NULL COMMENT 'User-Agent',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `reply`       TEXT         COMMENT '管理员回复内容',
    `reply_time`  DATETIME     DEFAULT NULL COMMENT '回复时间',
    `replied_by`  BIGINT       DEFAULT NULL COMMENT '回复管理员ID',
    PRIMARY KEY (`id`),
    KEY `idx_comments_goods` (`goods_id`),
    KEY `idx_comments_target` (`target_type`, `target_id`),
    KEY `idx_comments_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论表';

-- ------------------------------------------------------------
-- 9. 文件表（上传文件记录）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `files` (
    `id`             INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_name`      VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `original_name`  VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `file_path`      VARCHAR(500) DEFAULT NULL COMMENT '相对路径（配合 /img/ 前缀访问）',
    `file_size`      BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
    `file_type`      VARCHAR(64)  DEFAULT NULL COMMENT '文件类型',
    `extension`      VARCHAR(32)  DEFAULT NULL COMMENT '扩展名',
    `description`    VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `upload_user`    VARCHAR(64)  DEFAULT NULL COMMENT '上传人',
    `upload_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `download_count` INT          NOT NULL DEFAULT 0 COMMENT '下载次数',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除: 0 否, 1 是',
    PRIMARY KEY (`id`),
    KEY `idx_files_upload_user` (`upload_user`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件表';

-- ------------------------------------------------------------
-- 10. 标签表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tags` (
    `id`         INT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(64) NOT NULL COMMENT '标签名',
    `slug`       VARCHAR(64) DEFAULT NULL COMMENT '标签别名',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tags_slug` (`slug`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标签表';

-- ------------------------------------------------------------
-- 11. 文章表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `articles` (
    `id`                INT           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`           BIGINT        DEFAULT NULL COMMENT '作者用户ID',
    `title`             VARCHAR(200)  NOT NULL COMMENT '标题',
    `content`           MEDIUMTEXT    COMMENT '正文',
    `cover_image`       VARCHAR(500)  DEFAULT NULL COMMENT '封面图',
    `image_list`        TEXT          COMMENT '配图列表（JSON 数组）',
    `video_url`         VARCHAR(500)  DEFAULT NULL COMMENT '视频地址',
    `topic`             VARCHAR(100)  DEFAULT NULL COMMENT '话题/分类',
    `location`          VARCHAR(100)  DEFAULT NULL COMMENT '地点',
    `view_count`        INT           NOT NULL DEFAULT 0 COMMENT '浏览量',
    `published_at`      DATETIME      DEFAULT NULL COMMENT '发布时间',
    `status`            TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0 已删除/草稿, 1 已发布',
    `is_top`            TINYINT       NOT NULL DEFAULT 0 COMMENT '是否置顶: 0 否, 1 是',
    `related_goods_ids` VARCHAR(500)  DEFAULT NULL COMMENT '关联商品ID列表（JSON 数组，如 [1,2,3]）',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_articles_status` (`status`, `published_at`),
    KEY `idx_articles_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章表';

-- ------------------------------------------------------------
-- 12. 文章-商品关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `article_goods` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id` INT      NOT NULL COMMENT '文章ID',
    `goods_id`   BIGINT   NOT NULL COMMENT '商品ID',
    `sort`       INT      NOT NULL DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_goods` (`article_id`, `goods_id`),
    KEY `idx_article_goods_goods` (`goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章商品关联表';

-- ------------------------------------------------------------
-- 13. 记住我（自动登录）表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `remember_me` (
    `id`            INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(255) DEFAULT NULL COMMENT '密码哈希快照',
    `series`        VARCHAR(128) NOT NULL COMMENT '系列标识符',
    `token`         VARCHAR(255) DEFAULT NULL COMMENT '令牌值',
    `expire_time`   DATETIME     DEFAULT NULL COMMENT '过期时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_used`     DATETIME     DEFAULT NULL COMMENT '最后使用时间',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1 有效, 0 失效',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_remember_series` (`series`),
    KEY `idx_remember_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '自动登录表';


-- ============================================================
-- 演示种子数据
-- 账号（密码均为明文展示，数据库中存 BCrypt 哈希）：
--   admin   / admin123   （管理员）
--   seller1 / seller123  （卖家）
--   user1   / user123    （普通用户）
-- ============================================================

INSERT IGNORE INTO `user`
(`id`, `username`, `email`, `password_hash`, `avatar`, `role`, `status`, `bio`, `company_name`, `contact_phone`)
VALUES
(1, 'admin',   'admin@example.com',   '$2b$10$ffyhKpFzhIfTmCpSw5dWC.5Boi65e.e.5FMODQauwY6S2t2TsVBfq', NULL, 'ADMIN',  'ACTIVE', '系统管理员', NULL, NULL),
(2, 'seller1', 'seller1@example.com', '$2b$10$Z0jBdGc2oVAQdhmsj/XvQ.KM4wF1Qi8mlK8VPSriTlUXQi5jkuuh.', NULL, 'SELLER', 'ACTIVE', '数码好物店主', '星辰数码', '13800000001'),
(3, 'user1',   'user1@example.com',   '$2b$10$ldMYDKrjhIztLZK6ZWq8P.1jV.qB8Me6cuj8JA5qUoZV5pgUkj6eK', NULL, 'USER',   'ACTIVE', '热爱生活，热爱购物', NULL, NULL);

INSERT IGNORE INTO `categories` (`id`, `name`, `slug`, `description`, `parent_id`, `order_num`) VALUES
(1, '数码电器', 'electronics', '电脑、耳机、智能设备', 0, 1),
(2, '服饰鞋包', 'fashion',     '服装、鞋靴、箱包',     0, 2),
(3, '家居生活', 'home',        '家具、家饰、日用',     0, 3),
(4, '食品生鲜', 'food',        '零食、生鲜、饮品',     0, 4),
(5, '图书音像', 'books',       '图书、文具、音像',     0, 5),
(6, '美妆个护', 'beauty',      '护肤、彩妆、洗护',     0, 6);

INSERT IGNORE INTO `products`
(`id`, `name`, `description`, `main_image`, `image_list`, `price`, `original_price`, `stock`, `sales_count`, `category_id`, `status`, `seller_id`)
VALUES
(1, '轻薄商务笔记本电脑', '14 英寸 2.8K 屏，16G 内存 + 512G 固态，办公与轻度设计均适用。', '/img/laptop.png',    '["/img/laptop.png"]',    4999.00, 5999.00, 20,  35, 1, 1, 2),
(2, '真无线蓝牙耳机',     '主动降噪，单次续航 8 小时，配合充电仓可达 32 小时。',           '/img/earphones.png', '["/img/earphones.png"]',  399.00,  599.00,  50, 128, 1, 1, 2),
(3, '智能运动手表',       '心率 / 血氧监测，内置 GPS，支持 100+ 运动模式。',                '/img/smartwatch.jpg','["/img/smartwatch.jpg"]', 899.00, 1299.00, 30,  64, 1, 1, 2),
(4, '碎花雪纺连衣裙',     '春夏新款，收腰显瘦，面料轻薄透气。',                            '/img/dress.png',     '["/img/dress.png"]',      259.00,  399.00, 40,  87, 2, 1, 2),
(5, '男士纯棉商务衬衫',   '免烫抗皱，修身版型，通勤百搭。',                                  '/img/men_shirt.png', '["/img/men_shirt.png"]',  129.00,  199.00, 60,  45, 2, 1, 2),
(6, '北欧风陶瓷花瓶',     '手工拉坯，哑光釉面，适合客厅与桌面陈设。',                        '/img/ceramic_vase.png', '["/img/ceramic_vase.png"]', 168.00, 238.00, 25, 19, 3, 1, 2),
(7, '手工黄油曲奇礼盒',   '每日现烤，四种口味，节日伴手礼首选。',                            '/img/Handmade_cookies.png', '["/img/Handmade_cookies.png"]', 68.00, 98.00, 100, 210, 4, 1, 2),
(8, '经典心理学书单（5 册）', '涵盖社会心理学、认知心理学等经典入门读物。',                    '/img/Psychology_books.png', '["/img/Psychology_books.png"]', 129.00, 168.00, 80, 56, 5, 1, 2),
(9, '补水保湿面膜套装',   '玻尿酸补水，温和不刺激，一盒 20 片。',                            '/img/Facial_mask_suit.png', '["/img/Facial_mask_suit.png"]', 199.00, 299.00, 70, 132, 6, 1, 2);

INSERT IGNORE INTO `tags` (`id`, `name`, `slug`) VALUES
(1, '数码', 'digital'),
(2, '服饰', 'clothing'),
(3, '家居', 'home-living'),
(4, '图书', 'book');

INSERT IGNORE INTO `articles`
(`id`, `user_id`, `title`, `content`, `cover_image`, `topic`, `view_count`, `published_at`, `status`, `is_top`, `related_goods_ids`)
VALUES
(1, 1, '2026 春季数码好物推荐',
 '春天换新季，本文整理了笔记本、降噪耳机与智能手表三件日常高频好物，兼顾性能与性价比。\n\n一、轻薄本：办公与轻度创作足够用；\n二、降噪耳机：通勤地铁的刚需；\n三、智能手表：久坐提醒与运动记录的好帮手。',
 '/img/laptop.png', '数码', 0, NOW(), 1, 1, '[1,2,3]');

INSERT IGNORE INTO `article_goods` (`id`, `article_id`, `goods_id`, `sort`) VALUES
(1, 1, 1, 1),
(2, 1, 2, 2),
(3, 1, 3, 3);

INSERT IGNORE INTO `comments`
(`id`, `content`, `goods_id`, `username`, `rating`, `user_id`, `target_type`, `target_id`, `status`, `created_at`)
VALUES
(1, '耳机降噪效果很惊喜，通勤戴着很舒服。', 2, 'user1', 5, 3, 1, 2, 1, NOW()),
(2, '电脑很轻薄，屏幕色彩不错，日常办公完全够用。', 1, 'user1', 4, 3, 1, 1, 1, NOW());
