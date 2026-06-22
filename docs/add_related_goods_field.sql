-- =====================================================
-- 📝 文章表新增字段：关联商品ID
-- 执行时间: 2026-06-21
-- 用途: 支持手动指定推荐商品，替代复杂的智能匹配算法
-- 兼容: MySQL 5.7+ / MySQL 8.0+
-- =====================================================

USE shutiao;

-- 1️⃣ 添加 related_goods_ids 字段
-- 注意：MySQL不支持 IF NOT EXISTS，所以直接添加
-- 如果字段已存在会报错"Duplicate column name"，可以安全忽略
ALTER TABLE articles 
ADD COLUMN `related_goods_ids` VARCHAR(500) DEFAULT NULL 
COMMENT '关联的商品ID列表(JSON数组格式，如[1,2,3])' 
AFTER `is_top`;

-- 2️⃣ 验证字段是否添加成功
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'shutiao' 
  AND TABLE_NAME = 'articles' 
  AND COLUMN_NAME = 'related_goods_ids';

-- 3️⃣ 为现有文章初始化示例数据（可选）
-- 示例：为"真无线耳机横评"文章(ID=15)关联无线蓝牙耳机(ID=2)
UPDATE articles 
SET related_goods_ids = '[2]' 
WHERE id = 15;

-- 示例：为"人类简史读书笔记"(ID=10)关联书籍类商品
UPDATE articles 
SET related_goods_ids = '[3,12]' 
WHERE id = 10;

-- 示例：为"干皮春季底妆秘籍"(ID=3)关联美妆商品
UPDATE articles 
SET related_goods_ids = '[9,18]' 
WHERE id = 3;

-- 4️⃣ 验证更新结果
SELECT 
    id,
    title,
    topic,
    related_goods_ids
FROM articles 
WHERE status = 1 
  AND published_at IS NOT NULL 
LIMIT 20;

-- ✅ 完成！现在可以通过 related_goods_ids 字段直接管理关联商品了