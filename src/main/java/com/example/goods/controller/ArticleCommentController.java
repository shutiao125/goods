package com.example.goods.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.goods.Comment;
import com.example.goods.goods.User;
import com.example.goods.service.CommentService;
import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/articles")
public class ArticleCommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    /**
     * 📝 发表文章评论
     */
    @PostMapping("/{articleId}/comments")
    @ResponseBody
    public Map<String, Object> addComment(
            @PathVariable Integer articleId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId,
            Model model) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("💬 [ArticleComment] 收到新评论请求: 文章ID=" + articleId);

            // 获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                result.put("success", false);
                result.put("message", "请先登录后再评论");
                return result;
            }

            String username = auth.getName();
            User user = userService.getUserByUsername(username);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户信息异常");
                return result;
            }

            // 创建评论对象
            Comment comment = new Comment();
            comment.setContent(content);
            comment.setUserId(user.getId());
            comment.setUsername(username);
            comment.setTargetType((byte) 2); // 2表示文章评论
            comment.setTargetId(articleId.longValue());
            comment.setParentId(parentId != null ? parentId : 0L);
            comment.setStatus(1); // 1表示已审核通过
            comment.setCreatedAt(LocalDateTime.now());

            // 保存评论
            boolean saved = commentService.save(comment);

            if (saved) {
                result.put("success", true);
                result.put("message", "评论发表成功！");
                result.put("commentId", comment.getId());

                System.out.println("✅ [ArticleComment] 评论保存成功: ID=" + comment.getId() +
                                   ", 用户=" + username + ", 文章ID=" + articleId);
            } else {
                result.put("success", false);
                result.put("message", "评论保存失败");
                System.err.println("❌ [ArticleComment] 评论保存失败");
            }

        } catch (Exception e) {
            System.err.println("❌ [ArticleComment] 评论发表异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        return result;
    }

    /**
     * 📋 获取文章评论列表
     */
    @GetMapping("/{articleId}/comments")
    @ResponseBody
    public Map<String, Object> getComments(
            @PathVariable Integer articleId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("📋 [ArticleComment] 查询评论列表: 文章ID=" + articleId +
                               ", 页码=" + pageNum + ", 每页=" + pageSize);

            LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Comment::getTargetType, (byte) 2) // 文章评论
                   .eq(Comment::getTargetId, articleId.longValue())
                   .eq(Comment::getStatus, 1) // 已审核
                   .orderByDesc(Comment::getCreatedAt);

            Page<Comment> page = new Page<>(pageNum, pageSize);
            Page<Comment> commentPage = commentService.page(page, wrapper);

            result.put("success", true);
            result.put("data", commentPage.getRecords());
            result.put("total", commentPage.getTotal());
            result.put("pages", commentPage.getPages());
            result.put("current", commentPage.getCurrent());

            System.out.println("✅ [ArticleComment] 查询成功: 总数=" + commentPage.getTotal() +
                               ", 本页记录数=" + (commentPage.getRecords() != null ? commentPage.getRecords().size() : 0));

        } catch (Exception e) {
            System.err.println("❌ [ArticleComment] 查询评论失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取评论失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 🗑️ 删除文章评论
     */
    @DeleteMapping("/{articleId}/comments/{commentId}")
    @ResponseBody
    public Map<String, Object> deleteComment(
            @PathVariable Integer articleId,
            @PathVariable Long commentId) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🗑️ [ArticleComment] 删除评论: 文章ID=" + articleId + ", 评论ID=" + commentId);

            // 权限检查：只有管理员或评论作者可以删除
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            Comment comment = commentService.getById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }

            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            String username = auth.getName();

            if (!isAdmin && !username.equals(comment.getUsername())) {
                result.put("success", false);
                result.put("message", "您没有权限删除此评论");
                return result;
            }

            boolean deleted = commentService.removeById(commentId);

            if (deleted) {
                result.put("success", true);
                result.put("message", "评论删除成功");
                System.out.println("✅ [ArticleComment] 删除成功: 评论ID=" + commentId);
            } else {
                result.put("success", false);
                result.put("message", "删除失败");
            }

        } catch (Exception e) {
            System.err.println("❌ [ArticleComment] 删除评论异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        return result;
    }
}