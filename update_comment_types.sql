-- ============================================
-- 📝 更新评论类型字段（target_type）
-- 用于修复管理后台评论显示"⚠️ 待分类"的问题
-- ============================================

-- ⚠️ 执行前请先备份数据库！
-- BACKUP DATABASE your_database_name TO DISK = 'C:\backup\comments_backup.bak';

-- ============================================
-- 方案1：根据关联ID范围智能判断（推荐）
-- 假设：
--   - 商品ID范围: 1-1000（或更小）
--   - 文章ID范围: 1-100（或更大）
-- 请根据实际情况调整以下SQL语句
-- ============================================

-- 将所有未设置类型的评论标记为商品评论（如果target_id在商品ID范围内）
UPDATE comments
SET target_type = 1,
    updated_at = NOW()
WHERE (target_type IS NULL OR target_type = 0)
  AND target_id IS NOT NULL
  AND target_id <= 1000;  -- ← 根据实际商品数量调整此值

-- 将剩余的未分类评论标记为文章评论
UPDATE comments
SET target_type = 2,
    updated_at = NOW()
WHERE (target_type IS NULL OR target_type = 0)
  AND target_id IS NOT NULL;

-- ============================================
-- 方案2：手动指定特定评论的类型
-- 如果知道某些具体的评论ID，可以单独更新
-- ============================================

-- 示例：将评论ID为1-10的设置为文章评论
-- UPDATE comments SET target_type = 2 WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

-- 示例：将评论ID为11-20的设置为商品评论
-- UPDATE comments SET target_type = 1 WHERE id IN (11, 12, 13, 14, 15, 16, 17, 18, 19, 20);

-- ============================================
-- 方案3：通过goods_id字段判断（如果有该字段）
-- ============================================

-- 如果评论表有goods_id字段，且不为空则为商品评论
-- UPDATE comments
-- SET target_type = 1,
--     updated_at = NOW()
-- WHERE (target_type IS NULL OR target_type = 0)
--   AND goods_id IS NOT NULL
--   AND goods_id > 0;

-- ============================================
-- 验证更新结果
-- ============================================

-- 查看更新后的数据分布
SELECT
    CASE target_type
        WHEN 1 THEN '🛍️ 商品评论'
        WHEN 2 THEN '📝 文章评论'
        ELSE '❓ 未分类'
    END AS 评论类型,
    COUNT(*) AS 数量,
    MIN(id) AS 最小ID,
    MAX(id) AS 最大ID
FROM comments
GROUP BY target_type
ORDER BY target_type;

-- 查看仍然未分类的评论（应该为0）
SELECT id, username, content, target_id, created_at
FROM comments
WHERE target_type IS NULL OR target_type = 0
ORDER BY created_at DESC
LIMIT 20;

-- ============================================
-- 完成提示
-- ============================================
-- 执行完成后，刷新管理后台页面 /admin/comments
-- 所有评论应该显示正确的类型标签了！