-- =====================================================
-- 🔧 紧急修复：添加关联商品字段
-- 执行时间: 2026-06-21
-- 用途: 修复文章详情页500错误
-- =====================================================

USE shutiao;

-- 1️⃣ 检查字段是否存在（不会报错）
SELECT COUNT(*) AS field_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'shutiao' 
  AND TABLE_NAME = 'articles' 
  AND COLUMN_NAME = 'related_goods_ids';

-- 如果上面的查询返回0，说明字段不存在，需要执行下面的语句：

-- 2️⃣ 添加字段（如果上面返回0才执行）
ALTER TABLE articles 
ADD COLUMN `related_goods_ids` VARCHAR(500) DEFAULT NULL 
COMMENT '关联的商品ID列表(JSON数组格式)' 
AFTER `is_top`;

-- 3️⃣ 初始化示例数据
UPDATE articles SET related_goods_ids = '[2]' WHERE id = 15;
UPDATE articles SET related_goods_ids = '[3,12]' WHERE id = 10;
UPDATE articles SET related_goods_ids = '[9,18]' WHERE id = 3;

-- 4️⃣ 验证结果
SELECT id, title, related_goods_ids FROM articles WHERE id IN (3, 10, 15);

-- ✅ 完成！现在重启应用即可