package com.example.yy25gbs35_goods.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yy25gbs35_goods.config.FileUploadUtil;
import com.example.yy25gbs35_goods.goods.Article;
import com.example.yy25gbs35_goods.goods.Goods;
import com.example.yy25gbs35_goods.goods.User;
import com.example.yy25gbs35_goods.mapper.GoodsMapper;
import com.example.yy25gbs35_goods.service.ArticleService;
import com.example.yy25gbs35_goods.service.GoodsService;
import com.example.yy25gbs35_goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ArticleController {
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private GoodsService goodsService;
    
    @Autowired
    private GoodsMapper goodsMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FileUploadUtil fileUploadUtil;
    
    @GetMapping("/articles")
    public String listArticles(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String topic,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              Model model) {
        try {
            Integer pageSize = 9; // 文章列表每页显示9个

            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

            // 查询条件：只显示已发布的文章
            // status = 1 且 publishedAt <= 当前时间（定时发布的文章时间到了才显示）
            wrapper.eq(Article::getStatus, 1)
                   .isNotNull(Article::getPublishedAt)
                   .le(Article::getPublishedAt, java.time.LocalDateTime.now())
                   .orderByDesc(Article::getIsTop)
                   .orderByDesc(Article::getCreatedAt);

            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like(Article::getTitle, keyword)
                                 .or()
                                 .like(Article::getContent, keyword));
            }

            if (topic != null && !topic.trim().isEmpty()) {
                wrapper.eq(Article::getTopic, topic);
            }

            // 日期范围筛选
            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    java.time.LocalDateTime startDateTime = java.time.LocalDate.parse(startDate).atStartOfDay();
                    wrapper.ge(Article::getCreatedAt, startDateTime);
                } catch (Exception e) {
                    System.out.println("⚠️ [ArticleController] 开始日期格式错误: " + startDate);
                }
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    java.time.LocalDateTime endDateTime = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                    wrapper.le(Article::getCreatedAt, endDateTime);
                } catch (Exception e) {
                    System.out.println("⚠️ [ArticleController] 结束日期格式错误: " + endDate);
                }
            }
            
            Page<Article> page = new Page<>(pageNum, pageSize);
            Page<Article> articlePage = articleService.page(page, wrapper);
            
            // 获取当前登录用户信息，用于权限判断
            Long currentUserId = null;
            try {
                org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() &&
                    !authentication.getPrincipal().equals("anonymousUser")) {
                    String username = authentication.getName();
                    User currentUser = userService.getUserByUsername(username);
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ [ArticleController] 获取用户信息失败: " + e.getMessage());
            }
            
            // 为每篇文章设置是否为作者标志
            for (Article article : articlePage.getRecords()) {
                if (currentUserId != null && currentUserId.equals(article.getUserId())) {
                    article.setIsOwner(true);
                } else {
                    article.setIsOwner(false);
                }
            }
            
            System.out.println("📊 [ArticleController] 文章列表查询: 总数=" + articlePage.getTotal() + 
                             ", 当前页=" + articlePage.getCurrent() + 
                             ", 每页=" + articlePage.getSize() + 
                             ", 总页数=" + articlePage.getPages() +
                             ", 当前用户ID=" + currentUserId);
            
            model.addAttribute("articles", articlePage.getRecords());
            model.addAttribute("page", articlePage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("topic", topic);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("pageNum", pageNum);
            model.addAttribute("pageSize", pageSize);
            
            // 获取所有话题用于筛选
            List<String> topics = java.util.Arrays.asList(
                "美食探店", "旅行攻略", "美妆护肤", "数码科技",
                "家居生活", "读书笔记", "健身运动", "其他"
            );
            model.addAttribute("topics", topics);
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "加载文章列表失败：" + e.getMessage());
            model.addAttribute("articles", new ArrayList<>());
        }
        
        return "articles";
    }
    
    @GetMapping("/articles/{id}")
    public String articleDetail(@PathVariable Integer id, Model model) {
        try {
            System.out.println("📝 [ArticleController] 加载文章详情, ID: " + id);
            
            Article article = articleService.getArticleById(id);
            
            if (article == null) {
                System.out.println("⚠️ [ArticleController] 文章不存在: ID=" + id);
                model.addAttribute("message", "文章不存在或已删除");
                return "error";
            }
            
            System.out.println("✅ [ArticleController] 文章加载成功: " + article.getTitle());
            System.out.println("   📌 话题: " + article.getTopic());
            model.addAttribute("article", article);
            
            // 获取作者信息
            if (article.getUserId() != null) {
                try {
                    User author = userService.getUserById(article.getUserId());
                    model.addAttribute("author", author);
                    System.out.println("👤 [ArticleController] 作者信息: " + (author != null ? author.getUsername() : "未知"));
                } catch (Exception e) {
                    System.err.println("⚠️ [ArticleController] 获取作者信息失败: " + e.getMessage());
                }
            }
            
            // 🎯 获取关联商品（优先使用手动指定的商品ID）
            try {
                List<Goods> relatedGoods = getRelatedGoods(article);
                
                if (relatedGoods != null && !relatedGoods.isEmpty()) {
                    System.out.println("🛍️ [ArticleController] 关联到 " + relatedGoods.size() + " 个商品:");
                    for (Goods goods : relatedGoods) {
                        System.out.println("  ✅ " + goods.getName() + " (¥" + goods.getPrice() + ")");
                    }
                } else {
                    System.out.println("ℹ️ [ArticleController] 暂无关联商品");
                }
                
                model.addAttribute("relatedGoods", relatedGoods != null ? relatedGoods : new ArrayList<>());
            } catch (Exception e) {
                System.err.println("⚠️ [ArticleController] 获取关联商品失败: " + e.getMessage());
                model.addAttribute("relatedGoods", new ArrayList<>());
            }
            
        } catch (Exception e) {
            System.err.println("❌ [ArticleController] 加载文章详情失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "加载文章详情失败：" + e.getMessage());
            return "error";
        }
        
        return "article-detail";
    }
    
    /**
     * 🎯 获取关联商品（基于文章的 relatedGoodsIds 字段）
     * 简单可靠：直接根据ID查询，无需复杂算法
     */
    private List<Goods> getRelatedGoods(Article article) {
        List<Goods> relatedGoods = new ArrayList<>();
        
        try {
            String goodsIdsStr = null;
            
            try {
                goodsIdsStr = article.getRelatedGoodsIds();
            } catch (Exception e) {
                System.out.println("⚠️ [getRelatedGoods] 字段可能不存在: " + e.getMessage());
                return relatedGoods;
            }
            
            if (goodsIdsStr == null || goodsIdsStr.trim().isEmpty()) {
                System.out.println("ℹ️ [getRelatedGoods] 文章未设置关联商品ID");
                return relatedGoods;
            }
            
            System.out.println("📋 [getRelatedGoods] 解析商品ID: " + goodsIdsStr);
            
            // 解析JSON数组格式: [1,2,3]
            String cleanIds = goodsIdsStr.replace("[", "").replace("]", "").trim();
            
            if (cleanIds.isEmpty()) {
                return relatedGoods;
            }
            
            // 分割ID列表
            String[] idArray = cleanIds.split(",");
            List<Long> goodsIds = new ArrayList<>();
            
            for (String id : idArray) {
                try {
                    Long goodsId = Long.parseLong(id.trim());
                    goodsIds.add(goodsId);
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ [getRelatedGoods] 无效的商品ID: " + id);
                }
            }
            
            if (goodsIds.isEmpty()) {
                return relatedGoods;
            }
            
            // 根据ID列表查询商品
            LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Goods::getId, goodsIds)
                  .eq(Goods::getStatus, 1)
                  .orderByAsc(Goods::getId);
            
            relatedGoods = goodsMapper.selectList(wrapper);
            
            if (relatedGoods != null && !relatedGoods.isEmpty()) {
                System.out.println("✅ [getRelatedGoods] 成功加载 " + relatedGoods.size() + " 个关联商品:");
                for (Goods g : relatedGoods) {
                    System.out.println("   ✅ " + g.getName() + " (¥" + g.getPrice() + ")");
                }
            } else {
                System.out.println("⚠️ [getRelatedGoods] 未找到有效的上架商品");
            }
            
        } catch (Exception e) {
            System.err.println("❌ [getRelatedGoods] 获取关联商品失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return relatedGoods != null ? relatedGoods : new ArrayList<>();
    }
    
    // ========== 文章增删改查功能 ==========
    
    @GetMapping("/articles/create")
    public String createArticlePage(Model model) {
        model.addAttribute("article", new Article());
        model.addAttribute("topics", Arrays.asList(
            "美食探店", "旅行攻略", "美妆护肤", "数码科技", 
            "家居生活", "读书笔记", "健身运动", "其他"
        ));
        return "article-form";
    }
    
    @PostMapping("/articles/create")
    public String createArticle(Article article,
                               @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
                               @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                               Model model) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                model.addAttribute("error", "请先登录");
                return "redirect:/login";
            }
            
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                model.addAttribute("error", "用户不存在，请重新登录");
                return "redirect:/login";
            }
            
            // 设置文章作者为当前用户
            article.setUserId(currentUser.getId().longValue());

            // 如果用户没有选择状态，默认为已发布
            if (article.getStatus() == null) {
                article.setStatus(1);  // 默认已发布
            }

            System.out.println("📝 [ArticleController] 文章状态: " + (article.getStatus() == 1 ? "已发布" : "草稿"));

            article.setViewCount(0);
            article.setIsTop(0);
            article.setRelatedGoodsIds(null);
            
            System.out.println("👤 [ArticleController] 当前用户: " + username + " (ID: " + currentUser.getId() + ")");
            
            // 处理封面图片上传
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                String coverImageUrl = fileUploadUtil.uploadFile(coverImageFile);
                article.setCoverImage(coverImageUrl);
                System.out.println("📷 [ArticleController] 封面图片上传成功: " + coverImageUrl);
            }
            
            // 处理视频文件上传
            if (videoFile != null && !videoFile.isEmpty()) {
                String videoUrl = fileUploadUtil.uploadFile(videoFile);
                article.setVideoUrl(videoUrl);
                System.out.println("🎬 [ArticleController] 视频上传成功: " + videoUrl);
            }
            
            // 处理发布逻辑（尊重用户选择的状态）
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            
            // 记录用户选择的状态（在此之前的代码可能已经设置了默认值）
            Integer userSelectedStatus = article.getStatus();
            
            if (article.getPublishedAt() != null) {
                // 用户设置了发布时间
                if (article.getPublishedAt().isAfter(now)) {
                    // 未来时间 → 状态设为定时发布（2），等待定时任务处理
                    article.setStatus(2); 
                    System.out.println("⏰ [ArticleController] 设置定时发布时间: " + article.getPublishedAt());
                } else {
                    // 过去或当前时间 → 使用用户选择的状态
                    article.setPublishedAt(now);
                    // 如果用户没有明确选择，则默认发布
                    if (userSelectedStatus == null) {
                        article.setStatus(1);
                    }
                    System.out.println("✅ [ArticleController] 文章状态: " + (article.getStatus() == 1 ? "已发布" : "草稿"));
                }
            } else {
                // 未设置发布时间
                if (userSelectedStatus != null && userSelectedStatus == 0) {
                    // 用户明确选择草稿 → 不设置发布时间，保持草稿状态
                    article.setPublishedAt(null);
                    article.setStatus(0);
                    System.out.println("📝 [ArticleController] 保存为草稿（用户选择）");
                } else {
                    // 用户选择发布或未选择 → 立即发布
                    article.setPublishedAt(now);
                    article.setStatus(1);
                    System.out.println("🚀 [ArticleController] 立即发布文章");
                }
            }
            
            boolean saved = articleService.save(article);
            
            if (saved) {
                System.out.println("✅ [ArticleController] 文章创建成功: " + article.getTitle());
                return "redirect:/articles";
            } else {
                model.addAttribute("error", "文章创建失败");
                return "article-form";
            }
        } catch (Exception e) {
            System.err.println("❌ [ArticleController] 创建文章失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "创建失败：" + e.getMessage());
            return "article-form";
        }
    }
    
    @GetMapping("/articles/edit/{id}")
    public String editArticlePage(@PathVariable Integer id, Model model) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                model.addAttribute("error", "请先登录");
                return "redirect:/login";
            }
            
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                model.addAttribute("error", "用户不存在，请重新登录");
                return "redirect:/login";
            }
            
            Article article = articleService.getById(id);  // 直接查询，不过滤状态（管理员可编辑任何状态的文章）
            if (article == null) {
                model.addAttribute("error", "文章不存在");
                return "redirect:/articles";
            }
            
            // 权限检查：只有作者或管理员可以编辑
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                
            if (!isAdmin && !article.getUserId().equals(currentUser.getId().longValue())) {
                model.addAttribute("error", "您没有权限编辑此文章");
                System.out.println("⚠️ [ArticleController] 用户 " + username + " 尝试编辑不属于他的文章 ID=" + id);
                return "redirect:/articles";
            }
            
            model.addAttribute("article", article);
            model.addAttribute("topics", Arrays.asList(
                "美食探店", "旅行攻略", "美妆护肤", "数码科技", 
                "家居生活", "读书笔记", "健身运动", "其他"
            ));
            return "article-form";
        } catch (Exception e) {
            model.addAttribute("error", "加载文章失败：" + e.getMessage());
            return "redirect:/articles";
        }
    }
    
    @PostMapping("/articles/update/{id}")
    public String updateArticle(@PathVariable Integer id, Article article,
                               @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
                               @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                               Model model) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                model.addAttribute("error", "请先登录");
                return "redirect:/login";
            }
            
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                model.addAttribute("error", "用户不存在，请重新登录");
                return "redirect:/login";
            }
            
            article.setId(id);
            
            // 获取旧文章信息
            Article oldArticle = articleService.getById(id);
            
            if (oldArticle == null) {
                model.addAttribute("error", "文章不存在");
                return "redirect:/articles";
            }
            
            // 权限检查：只有作者或管理员可以编辑
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                
            if (!isAdmin && !oldArticle.getUserId().equals(currentUser.getId().longValue())) {
                model.addAttribute("error", "您没有权限编辑此文章");
                System.out.println("⚠️ [ArticleController] 用户 " + username + " 尝试更新不属于他的文章 ID=" + id);
                return "redirect:/articles";
            }
            
            System.out.println("👤 [ArticleController] 用户 " + username + " 正在更新文章 ID=" + id);
            
            // 处理封面图片上传
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                // 删除旧图片
                if (oldArticle.getCoverImage() != null && !oldArticle.getCoverImage().isEmpty()) {
                    fileUploadUtil.deleteFile(oldArticle.getCoverImage());
                    System.out.println("🗑️ [ArticleController] 删除旧封面图: " + oldArticle.getCoverImage());
                }
                
                // 上传新图片
                String coverImageUrl = fileUploadUtil.uploadFile(coverImageFile);
                article.setCoverImage(coverImageUrl);
                System.out.println("📷 [ArticleController] 新封面图片上传成功: " + coverImageUrl);
            } else {
                // 如果没有上传新图片，保留旧图片
                article.setCoverImage(oldArticle.getCoverImage());
            }
            
            // 处理视频文件上传
            if (videoFile != null && !videoFile.isEmpty()) {
                // 删除旧视频
                if (oldArticle.getVideoUrl() != null && !oldArticle.getVideoUrl().isEmpty()) {
                    fileUploadUtil.deleteFile(oldArticle.getVideoUrl());
                    System.out.println("🗑️ [ArticleController] 删除旧视频: " + oldArticle.getVideoUrl());
                }
                
                // 上传新视频
                String videoUrl = fileUploadUtil.uploadFile(videoFile);
                article.setVideoUrl(videoUrl);
                System.out.println("🎬 [ArticleController] 新视频上传成功: " + videoUrl);
            } else {
                // 如果没有上传新视频，保留旧视频
                article.setVideoUrl(oldArticle.getVideoUrl());
            }
            
            // 保留原有的 userId 和其他不可修改字段
            article.setUserId(oldArticle.getUserId());
            article.setCreatedAt(oldArticle.getCreatedAt());

            // 如果用户没有修改状态，保留原来的状态
            if (article.getStatus() == null) {
                article.setStatus(oldArticle.getStatus());
                System.out.println("📝 [ArticleController] 保留原文章状态: " + (oldArticle.getStatus() == 1 ? "已发布" : "草稿"));
            } else {
                System.out.println("📝 [ArticleController] 文章状态已更新: " + (article.getStatus() == 1 ? "已发布" : "草稿"));
            }

            boolean updated = articleService.updateById(article);

            if (updated) {
                System.out.println("✅ [ArticleController] 文章更新成功: ID=" + id);

                // 根据用户角色跳转到不同页面
                if (isAdmin) {
                    System.out.println("👨‍💼 [ArticleController] 管理员编辑完成，返回管理后台");
                    return "redirect:/admin/articles";
                } else {
                    System.out.println("👤 [ArticleController] 普通用户编辑完成，返回文章列表");
                    return "redirect:/articles";
                }
            } else {
                model.addAttribute("error", "更新失败");
                return "article-form";
            }
        } catch (Exception e) {
            System.err.println("❌ [ArticleController] 更新文章失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "更新失败：" + e.getMessage());
            return "article-form";
        }
    }
    
    @PostMapping("/articles/delete/{id}")
    public String deleteArticle(@PathVariable Integer id, Model model) {
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                model.addAttribute("error", "请先登录");
                return "redirect:/login";
            }
            
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);
            
            Article article = articleService.getById(id);
            if (article != null) {
                // 权限检查：只有作者或管理员可以删除
                boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

                if (!isAdmin && currentUser != null && !article.getUserId().equals(currentUser.getId().longValue())) {
                    model.addAttribute("error", "您没有权限删除此文章");
                    System.out.println("⚠️ [ArticleController] 用户 " + username + " 尝试删除不属于他的文章 ID=" + id);
                    return "redirect:/articles";
                }

                // 物理删除文章（从数据库中彻底移除）
                String articleTitle = article.getTitle();
                boolean deleted = articleService.removeById(id);

                if (deleted) {
                    System.out.println("✅ [ArticleController] 文章物理删除成功: ID=" + id + ", 标题=" + articleTitle);
                    return "redirect:/articles";
                } else {
                    System.err.println("❌ [ArticleController] 文章删除失败: ID=" + id);
                    model.addAttribute("error", "删除失败");
                    return "redirect:/articles";
                }
            }
            
            model.addAttribute("error", "删除失败");
            return "redirect:/articles";
        } catch (Exception e) {
            System.err.println("❌ [ArticleController] 删除文章失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "删除失败：" + e.getMessage());
            return "redirect:/articles";
        }
    }
    
    /**
     * 📝 更新文章的关联商品ID
     */
    @PostMapping("/articles/{articleId}/update-goods")
    @ResponseBody
    public Map<String, Object> updateRelatedGoods(
            @PathVariable Integer articleId,
            @RequestParam String goodsIds) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Article article = articleService.getById(articleId);
            if (article == null) {
                result.put("success", false);
                result.put("message", "文章不存在");
                return result;
            }
            
            article.setRelatedGoodsIds(goodsIds);
            boolean updated = articleService.updateById(article);
            
            if (updated) {
                System.out.println("✅ [ArticleController] 更新文章 " + articleId + " 的关联商品: " + goodsIds);
                result.put("success", true);
                result.put("message", "关联商品更新成功！");
            } else {
                result.put("success", false);
                result.put("message", "更新失败，请重试");
            }
        } catch (Exception e) {
            System.err.println("❌ [ArticleController] 更新关联商品失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }
    
    private List<String> getDistinctTopics(List<Article> articles) {
        Map<String, String> topicMap = new HashMap<>();
        for (Article article : articles) {
            if (article.getTopic() != null && !article.getTopic().trim().isEmpty()) {
                topicMap.put(article.getTopic(), article.getTopic());
            }
        }
        return new ArrayList<>(topicMap.keySet());
    }

    /**
     * 批量删除文章（仅管理员）
     */
    @PostMapping("/articles/batch-delete")
    @ResponseBody
    public Map<String, Object> batchDeleteArticles(@RequestBody Map<String, List<Integer>> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查管理员权限
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                result.put("success", false);
                result.put("message", "只有管理员才能批量删除文章");
                return result;
            }

            List<Integer> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要删除的文章");
                return result;
            }

            System.out.println("🗑️ [批量删除] 管理员开始删除 " + ids.size() + " 篇文章: " + ids);

            int deletedCount = 0;

            for (Integer id : ids) {
                try {
                    Article article = articleService.getById(id);
                    if (article != null && article.getStatus() != null && article.getStatus() > 0) {
                        boolean deleted = articleService.removeById(id);
                        if (deleted) {
                            deletedCount++;
                            System.out.println("  ✅ 删除成功: ID=" + id + ", 标题=" + article.getTitle());
                        } else {
                            System.out.println("  ❌ 删除失败: ID=" + id);
                        }
                    } else {
                        System.out.println("  ⚠️ 文章不存在或已删除: ID=" + id);
                    }
                } catch (Exception e) {
                    System.err.println("  ❌ 删除文章异常: ID=" + id + ", 错误=" + e.getMessage());
                    e.printStackTrace();
                }
            }

            result.put("success", true);
            result.put("deletedCount", deletedCount);
            result.put("message", "成功删除 " + deletedCount + " 篇文章");

            System.out.println("✅ [批量删除] 完成：成功删除 " + deletedCount + "/" + ids.size() + " 篇文章");

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "批量删除失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 批量下载文章（导出为JSON文件）
     */
    @PostMapping("/articles/batch-download")
    @ResponseBody
    public ResponseEntity<byte[]> batchDownloadArticles(@RequestBody Map<String, List<Integer>> request) {
        try {
            // 检查管理员权限
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body("请先登录".getBytes());
            }

            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return ResponseEntity.status(403).body("只有管理员才能下载文章".getBytes());
            }

            List<Integer> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("请选择要下载的文章".getBytes());
            }

            System.out.println("📥 [批量下载] 管理员准备下载 " + ids.size() + " 篇文章");

            // 查询选中的文章
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Article::getId, ids)
                   .eq(Article::getStatus, 1);  // 只导出未删除的文章

            List<Article> articles = articleService.list(wrapper);

            if (articles.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 转换为JSON格式（去除敏感信息）
            java.util.List<Map<String, Object>> exportData = new java.util.ArrayList<>();
            for (Article article : articles) {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", article.getId());
                item.put("title", article.getTitle());
                item.put("content", article.getContent());
                item.put("topic", article.getTopic());
                item.put("coverImage", article.getCoverImage());
                item.put("location", article.getLocation());
                item.put("createdAt", article.getCreatedAt() != null ? article.getCreatedAt().toString() : "");
                item.put("publishedAt", article.getPublishedAt() != null ? article.getPublishedAt().toString() : "");
                item.put("viewCount", article.getViewCount());
                item.put("isTop", article.getIsTop());

                exportData.add(item);
            }

            // 转换为JSON字符串
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
            byte[] jsonBytes = json.getBytes("UTF-8");

            // 设置响应头
            String dateStr = java.time.LocalDate.now().toString();
            String fileName = "articles_" + dateStr + "_" + articles.size() + "篇.json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBytes);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                return ResponseEntity.internalServerError().body(("下载失败：" + e.getMessage()).getBytes("UTF-8"));
            } catch (Exception ex) {
                return ResponseEntity.internalServerError().build();
            }
        }
    }
}