package com.example.goods.controller;



import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.goods.*;
import com.example.goods.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private TagService tagService;
    @Autowired
    private CommentService commentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private com.example.goods.mapper.OrderItemMapper orderItemMapper;

    @Autowired
    private com.example.goods.service.ArticleService articleService;

    @Autowired
    private com.example.goods.mapper.ArticleMapper articleMapper;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("goodsCount", goodsService.count());
        model.addAttribute("userCount", userService.count());
        model.addAttribute("fileCount", fileService.count());
        model.addAttribute("categoryCount", categoryService.count());
        model.addAttribute("tagCount", tagService.count());
        model.addAttribute("commentCount", commentService.count());

        // 查询文章数量统计
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Article> articleWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            articleWrapper.eq(Article::getStatus, 1);
            long articleCount = articleMapper.selectCount(articleWrapper);
            model.addAttribute("articleCount", articleCount);
            System.out.println("📝 文章总数: " + articleCount);
        } catch (Exception e) {
            System.err.println("❌ 查询文章数量失败:");
            e.printStackTrace();
            model.addAttribute("articleCount", 0);
        }

        // 查询今日订单和收入统计
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDateTime startOfDay = today.atStartOfDay();
            java.time.LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

            // 查询今日订单数量（已付款、已发货、已完成状态）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderInfo> orderWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            orderWrapper.ge(OrderInfo::getCreatedAt, startOfDay);
            orderWrapper.lt(OrderInfo::getCreatedAt, endOfDay);
            orderWrapper.in(OrderInfo::getStatus, 1, 2, 3); // 待发货、已发货、已完成
            long todayOrderCount = orderService.count(orderWrapper);
            model.addAttribute("todayOrderCount", todayOrderCount);

            // 查询今日收入（已完成订单的支付金额总和）
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderInfo> incomeWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            incomeWrapper.ge(OrderInfo::getCreatedAt, startOfDay);
            incomeWrapper.lt(OrderInfo::getCreatedAt, endOfDay);
            incomeWrapper.eq(OrderInfo::getStatus, 3); // 只计算已完成的订单
            List<OrderInfo> todayCompletedOrders = orderService.list(incomeWrapper);

            java.math.BigDecimal todayIncome = java.math.BigDecimal.ZERO;
            if (todayCompletedOrders != null) {
                for (OrderInfo order : todayCompletedOrders) {
                    if (order.getPayAmount() != null) {
                        todayIncome = todayIncome.add(order.getPayAmount());
                    }
                }
            }
            model.addAttribute("todayIncome", todayIncome);

            System.out.println("\n========== 控制台统计数据 ==========");
            System.out.println("📊 今日订单数: " + todayOrderCount);
            System.out.println("💰 今日收入: ¥" + todayIncome);
            System.out.println("====================================\n");

        } catch (Exception e) {
            System.err.println("❌ 查询今日统计数据失败:");
            e.printStackTrace();
            model.addAttribute("todayOrderCount", 0);
            model.addAttribute("todayIncome", java.math.BigDecimal.ZERO);
        }

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String userManage(@RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "10") int pageSize,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String role,
                             @RequestParam(required = false) String status,
                             Model model) {
        // 统一从 user 表查询所有类型用户
        Page<User> userPage = userService.getUsersByPage(pageNum, pageSize, keyword, role, status);
        model.addAttribute("users", userPage.getRecords());
        model.addAttribute("page", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        // 统计各角色用户数量
        model.addAttribute("totalUserCount", userService.count());
        model.addAttribute("adminCount", userService.countByRole("ADMIN"));
        model.addAttribute("normalUserCount", userService.countByRole("USER"));
        model.addAttribute("sellerCount", userService.countByRole("SELLER"));

        return "admin/user-manage";
    }

    @PostMapping("/users/add")
    @ResponseBody
    public boolean addUser(@RequestBody User user) {
        return userService.addUser(user);
    }

    @PostMapping("/users/update")
    @ResponseBody
    public boolean updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }

    @PostMapping("/users/delete/{id}")
    @ResponseBody
    public boolean deleteUser(@PathVariable Integer id) {
        return userService.deleteUser(id);
    }

    @PostMapping("/users/delete-batch")
    @ResponseBody
    public boolean deleteUsers(@RequestBody List<Integer> ids) {
        return userService.deleteUsers(ids);
    }

    @PostMapping("/users/update-status")
    @ResponseBody
    public boolean updateUserStatus(@RequestParam Integer id, @RequestParam String status) {
        return userService.updateUserStatus(id, status);
    }

    @PostMapping("/users/update-role")
    @ResponseBody
    public boolean updateUserRole(@RequestParam Integer id, @RequestParam String role) {
        return userService.updateUserRole(id, role);
    }

    @GetMapping("/categories")
    public String categoryManage(@RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String keyword,
                                 Model model) {
        model.addAttribute("categoryPage", categoryService.getCategoriesByPage(pageNum, pageSize, keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("allCategories", categoryService.getAllCategories());
        return "admin/category-manage";
    }

    @PostMapping("/categories/add")
    @ResponseBody
    public boolean addCategory(@RequestBody Category category) {
        return categoryService.addCategory(category);
    }

    @PostMapping("/categories/update")
    @ResponseBody
    public boolean updateCategory(@RequestBody Category category) {
        return categoryService.updateCategory(category);
    }

    @PostMapping("/categories/delete/{id}")
    @ResponseBody
    public boolean deleteCategory(@PathVariable Integer id) {
        return categoryService.deleteCategory(id);
    }

    @PostMapping("/categories/delete-batch")
    @ResponseBody
    public boolean deleteCategories(@RequestBody List<Integer> ids) {
        return categoryService.deleteCategories(ids);
    }

    @GetMapping("/tags")
    public String tagManage(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            @RequestParam(required = false) String keyword,
                            Model model) {
        model.addAttribute("tagPage", tagService.getTagsByPage(pageNum, pageSize, keyword));
        model.addAttribute("keyword", keyword);
        return "admin/tag-manage";
    }

    @PostMapping("/tags/add")
    @ResponseBody
    public boolean addTag(@RequestBody Tag tag) {
        return tagService.addTag(tag);
    }

    @PostMapping("/tags/update")
    @ResponseBody
    public boolean updateTag(@RequestBody Tag tag) {
        return tagService.updateTag(tag);
    }

    @PostMapping("/tags/delete/{id}")
    @ResponseBody
    public boolean deleteTag(@PathVariable Integer id) {
        return tagService.deleteTag(id);
    }

    @PostMapping("/tags/delete-batch")
    @ResponseBody
    public boolean deleteTags(@RequestBody List<Integer> ids) {
        return tagService.deleteTags(ids);
    }

    @GetMapping("/goods")
    public String goodsManage(@RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "12") int pageSize,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Integer categoryId,
                             @RequestParam(required = false) Integer status,
                             Model model) {
        model.addAttribute("goodsPage", goodsService.getGoodsByPage(pageNum, pageSize, keyword, categoryId, status));
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/goods-manage";
    }

    @GetMapping("/comments")
    public String commentManage(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) Long goodsId,
                                @RequestParam(required = false) String targetType,
                                Model model) {
        model.addAttribute("commentPage", commentService.getCommentsByPage(pageNum, pageSize, status, goodsId, targetType));
        model.addAttribute("status", status);
        model.addAttribute("goodsId", goodsId);
        model.addAttribute("targetType", targetType);
        return "admin/comment-manage";
    }

    @PostMapping("/comments/approve/{id}")
    @ResponseBody
    public boolean approveComment(@PathVariable Long id) {
        return commentService.approveComment(id);
    }

    @PostMapping("/comments/reject/{id}")
    @ResponseBody
    public boolean rejectComment(@PathVariable Long id) {
        return commentService.rejectComment(id);
    }

    @PostMapping("/comments/delete/{id}")
    @ResponseBody
    public boolean deleteComment(@PathVariable Long id) {
        return commentService.deleteComment(id);
    }

    @PostMapping("/comments/delete-batch")
    @ResponseBody
    public boolean deleteComments(@RequestBody List<Long> ids) {
        return commentService.deleteComments(ids);
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public String orderManage(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Integer status,
                              @RequestParam(required = false) String keyword,
                              Model model) {
        model.addAttribute("orderPage", orderService.getAllOrders(pageNum, pageSize, status, keyword));
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        return "admin/order-manage";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        OrderInfo order = orderService.getOrderDetail(id, null);
        if (order == null) {
            return "redirect:/admin/orders";
        }
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, id);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        return "admin/order-detail";
    }

    @PostMapping("/orders/ship")
    @ResponseBody
    public Map<String, Object> shipOrder(@RequestParam Long orderId,
                                         @RequestParam String expressCompany,
                                         @RequestParam String expressNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = orderService.shipOrder(orderId, expressCompany, expressNo);
            result.put("success", success);
            result.put("message", success ? "发货成功" : "发货失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 评论回复 ====================

    @PostMapping("/comments/reply/{id}")
    @ResponseBody
    public Map<String, Object> replyComment(@PathVariable Long id,
                                          @RequestParam String replyContent) {
        Map<String, Object> result = new HashMap<>();
        try {
            Comment comment = commentService.getCommentById(id);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }
            comment.setReply(replyContent);
            comment.setReplyTime(java.time.LocalDateTime.now());
            boolean success = commentService.updateComment(comment);
            result.put("success", success);
            result.put("message", success ? "回复成功" : "回复失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 管理员取消订单（修复版）
     * 路径：/admin/orders/{id}/cancel
     */
    @PostMapping("/orders/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adminCancelOrder(@PathVariable Long id) {
        System.out.println("\n========== 管理员取消订单 (AdminController) ==========");
        System.out.println("🗑️ [AdminController] 取消订单, orderId: " + id);

        Map<String, Object> result = new HashMap<>();

        try {
            com.example.goods.goods.OrderInfo order = orderService.getById(id);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return ResponseEntity.ok().body(result);
            }

            System.out.println("📋 [AdminController] 订单状态: " + order.getStatus());

            if (order.getStatus() != 0 && order.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "只能取消待付款或待发货的订单(当前状态:" + order.getStatus() + ")");
                return ResponseEntity.ok().body(result);
            }

            System.out.println("✅ [AdminController] 开始执行取消操作...");
            boolean success = orderService.adminCancelOrder(id);
            result.put("success", success);
            result.put("message", success ? "订单已取消" : "取消失败");

            System.out.println("✅ [AdminController] 取消结果: " + success);

        } catch (Exception e) {
            System.err.println("❌ [AdminController] 异常:");
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        System.out.println("📤 [AdminController] 返回数据: " + result);
        System.out.println("================================================\n");

        return ResponseEntity.ok().body(result);
    }

    /**
     * 管理员完成订单（将已发货订单标记为已完成）
     * 路径：/admin/orders/{id}/complete
     */
    @PostMapping("/orders/{id}/complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adminCompleteOrder(@PathVariable Long id) {
        System.out.println("\n========== 管理员完成订单 (AdminController) ==========");
        System.out.println("🎉 [AdminController] 完成订单, orderId: " + id);

        Map<String, Object> result = new HashMap<>();

        try {
            com.example.goods.goods.OrderInfo order = orderService.getById(id);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return ResponseEntity.ok().body(result);
            }

            System.out.println("📋 [AdminController] 订单状态: " + order.getStatus());

            if (order.getStatus() != 2) {
                result.put("success", false);
                result.put("message", "只能完成已发货的订单(当前状态:" + order.getStatus() + ")");
                return ResponseEntity.ok().body(result);
            }

            System.out.println("✅ [AdminController] 开始执行完成操作...");
            boolean success = orderService.completeOrder(id);
            result.put("success", success);
            result.put("message", success ? "订单已完成" : "操作失败");

            System.out.println("✅ [AdminController] 完成结果: " + success);

        } catch (Exception e) {
            System.err.println("❌ [AdminController] 异常:");
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        System.out.println("📤 [AdminController] 返回数据: " + result);
        System.out.println("=====================================================\n");

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/articles")
    public String articleManage(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String topic,
                                Model model) {
        try {
            System.out.println("📝 [AdminController] 加载文章管理页面");
            System.out.println("   页码: " + pageNum + ", 每页: " + pageSize);
            if (keyword != null && !keyword.isEmpty()) {
                System.out.println("   搜索关键词: " + keyword);
            }
            if (topic != null && !topic.isEmpty()) {
                System.out.println("   筛选分类: " + topic);
            }

            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Article> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.ge(Article::getStatus, 0);  // 显示所有文章：已发布(1)、草稿(0)、定时发布(2)

            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like(Article::getTitle, keyword)
                                 .or()
                                 .like(Article::getContent, keyword));
            }

            if (topic != null && !topic.isEmpty() && !"全部".equals(topic)) {
                wrapper.eq(Article::getTopic, topic);
            }

            wrapper.orderByDesc(Article::getCreatedAt);

            Page<Article> page = new Page<>(pageNum, pageSize);
            Page<Article> articlePage = articleMapper.selectPage(page, wrapper);

            model.addAttribute("articles", articlePage.getRecords());
            model.addAttribute("page", articlePage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("topic", topic);
            model.addAttribute("topics", java.util.Arrays.asList(
                "美食探店", "旅行攻略", "美妆护肤", "数码科技",
                "家居生活", "读书笔记", "健身运动", "其他"
            ));

            long totalCount = articlePage.getTotal();
            System.out.println("✅ [AdminController] 文章管理加载成功，共 " + totalCount + " 篇文章");

            return "admin/article-manage";

        } catch (Exception e) {
            System.err.println("❌ [AdminController] 加载文章管理失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "加载文章列表失败: " + e.getMessage());
            return "admin/article-manage";
        }
    }

    @PostMapping("/articles/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteArticle(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🗑️ [AdminController] 删除文章 ID: " + id);

            Article article = articleService.getById(id);
            if (article == null) {
                result.put("success", false);
                result.put("message", "文章不存在");
                return result;
            }

            // 真正删除文章（从数据库中移除）
            boolean success = articleService.removeById(id);

            if (success) {
                result.put("success", true);
                result.put("message", "文章删除成功！");
                System.out.println("✅ [AdminController] 文章删除成功: " + article.getTitle());
            } else {
                result.put("success", false);
                result.put("message", "删除失败，请重试");
                System.err.println("❌ [AdminController] 文章删除失败");
            }

        } catch (Exception e) {
            System.err.println("❌ [AdminController] 删除文章异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误: " + e.getMessage());
        }

        return result;
    }
}