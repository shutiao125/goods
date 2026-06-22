-- ============================================
-- 🧹 数据库清理脚本：删除已标记为删除的文章
-- 执行时间: 2026-06-22
-- 用途: 清理之前使用软删除（status=0）遗留的数据
-- ============================================

-- 1. 查看当前有多少已"软删除"的文章（status=0）
SELECT COUNT(*) AS '待清理的已删除文章数'
FROM articles
WHERE status = 0;

-- 2. 查看这些被"软删除"的文章详情（用于确认）
SELECT id, title, user_id, status, created_at, updated_at
FROM articles
WHERE status = 0
ORDER BY updated_at DESC;

-- ⚠️ 以下操作会永久删除数据，请先执行上面的查询确认！
-- 确认无误后，取消注释下面的语句并执行：

-- 3. 物理删除所有 status=0 的文章记录
-- DELETE FROM articles WHERE status = 0;

-- 4. 验证删除结果
-- SELECT COUNT(*) AS '剩余文章总数' FROM articles;
-- SELECT COUNT(*) AS '仍存在的已删除文章数' FROM articles WHERE status = 0; -- 应该返回 0

-- ============================================
-- ✅ 清理完成提示：
-- 如果第4步返回0，说明所有已删除的文章已被彻底清除
-- ============================================