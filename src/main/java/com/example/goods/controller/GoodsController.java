package com.example.goods.controller;

import com.example.goods.config.FileUploadUtil;
import com.example.goods.goods.Comment;
import com.example.goods.goods.Files;
import com.example.goods.goods.Goods;
import com.example.goods.goods.User;
import com.example.goods.service.AsyncFileService;
import com.example.goods.service.CommentService;
import com.example.goods.service.FileService;
import com.example.goods.service.GoodsService;
import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GoodsController {
    @Autowired
    private GoodsService goodsService;
    
    @Autowired
    private CommentService commentService;

    @Autowired
    private FileUploadUtil fileUploadUtil;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private AsyncFileService asyncFileService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/goods")
    @ResponseBody
    public List<Goods> getGoods(){
        return goodsService.getAllGoods();
    }

    @GetMapping("/goods/list")
    public String goodsList(Model model) {
        try {
            System.out.println("🛍️ [GoodsController] 加载商品列表");
            List<Goods> goodsList = goodsService.getAllGoods();
            
            if (goodsList != null && !goodsList.isEmpty()) {
                model.addAttribute("goods", goodsList);
                System.out.println("✅ [GoodsController] 成功加载 " + goodsList.size() + " 个商品");
            } else {
                model.addAttribute("goods", new ArrayList<>());
                System.out.println("⚠️ [GoodsController] 商品列表为空");
            }
            
            return "goods-list";
        } catch (Exception e) {
            System.err.println("❌ [GoodsController] 加载商品列表失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "加载商品列表失败：" + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/goods/{id:[\\d]+}")
    public String goodsDetail(@PathVariable Long id, Model model){
        try {
            Goods goods = goodsService.getGoodsById(id);
            if (goods == null) {
                model.addAttribute("message", "商品不存在或未上架");
                return "error";
            }
            model.addAttribute("goods", goods);
            
            List<Comment> comments = commentService.getCommentsByGoodsId(id);
            model.addAttribute("comments", comments);
            
            int commentCount = comments != null ? comments.size() : 0;
            model.addAttribute("commentCount", commentCount);
            
            model.addAttribute("newComment", new Comment());
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "加载商品详情失败：" + e.getMessage());
            return "error";
        }
        return "goods-detail";
    }

    @PostMapping("/goods/{id}/comment")
    public String addComment(@PathVariable Long id, Comment comment, @RequestParam String username, @RequestParam(required = false) Integer rating) {
        try {
            comment.setGoodsId(id);
            comment.setUsername(username);
            comment.setRating(rating != null ? rating : 5);
            comment.setStatus(0); // 待审核状态
            comment.setCreatedAt(LocalDateTime.now());
            
            // 为数据库中的必填字段设置默认值
            if (comment.getUserId() == null) {
                comment.setUserId(0L); // 匿名用户ID设为0
            }
            if (comment.getTargetType() == null) {
                comment.setTargetType((byte) 1); // 1表示商品类型
            }
            if (comment.getTargetId() == null) {
                comment.setTargetId(id); // 目标ID与商品ID相同
            }
            if (comment.getParentId() == null) {
                comment.setParentId(0L); // 顶级评论
            }
            
            commentService.save(comment);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/goods/" + id;
    }

    @GetMapping("/goods/publish")
    public String publishPage(Model model) {
        model.addAttribute("goods", new Goods());
        return "goods-publish";
    }

    @PostMapping("/goods/upload-image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String filePath = fileUploadUtil.uploadFile(file);
            result.put("success", true);
            result.put("url", filePath);
            result.put("message", "上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取本地图片列表（从 static/img 目录）
     */
    @GetMapping("/api/local-images")
    @ResponseBody
    public Map<String, Object> getLocalImages() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用类路径下的 static/img 目录
            String localPicDir = "src/main/resources/static/img";
            File dir = new File(localPicDir);
            
            if (!dir.exists()) {
                result.put("success", false);
                result.put("message", "本地图片目录不存在: " + localPicDir);
                return result;
            }
            
            // 获取所有图片文件
            File[] imageFiles = dir.listFiles((d, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".jpg") || 
                       lowerName.endsWith(".jpeg") || 
                       lowerName.endsWith(".png") || 
                       lowerName.endsWith(".gif") || 
                       lowerName.endsWith(".bmp") ||
                       lowerName.endsWith(".webp");
            });
            
            if (imageFiles == null || imageFiles.length == 0) {
                result.put("success", true);
                result.put("images", new java.util.ArrayList<>());
                result.put("message", "目录下没有图片");
                return result;
            }
            
            // 构建图片信息列表
            java.util.List<Map<String, Object>> images = new java.util.ArrayList<>();
            for (File file : imageFiles) {
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("fileName", file.getName());
                imageInfo.put("filePath", file.getAbsolutePath());
                imageInfo.put("fileSize", file.length());
                imageInfo.put("lastModified", file.lastModified());
                
                // 生成预览URL（通过静态资源访问）
                imageInfo.put("previewUrl", "/img/" + file.getName());
                
                images.add(imageInfo);
            }
            
            result.put("success", true);
            result.put("images", images);
            result.put("count", images.size());
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "读取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 访问本地图片（用于预览）
     */
    @GetMapping("/local-pic/{fileName}")
    public ResponseEntity<Resource> viewLocalPic(@PathVariable String fileName) {
        try {
            String filePath = "D:\\pic\\" + fileName;
            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = determineContentType(fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件名确定 Content-Type
     */
    private String determineContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    @GetMapping("/goods/download-image/{fileName}")
    public ResponseEntity<Resource> downloadImage(@PathVariable String fileName) throws UnsupportedEncodingException {
        try {
            // 从 static/img 目录读取文件
            String filePath = "src/main/resources/static/img/" + fileName;
            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/goods/publish")
    public String publishGoods(Goods goods,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Model model) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                Files fileInfo = fileService.uploadFile(imageFile, "admin", "商品封面图");
                // 使用 /img/ 前缀匹配静态资源映射
                String imagePath = "/img/" + fileInfo.getFilePath();
                goods.setMainImage(imagePath);
            }
            goodsService.saveGoods(goods);
            return "redirect:/";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "发布失败：" + e.getMessage());
            model.addAttribute("goods", goods);
            return "goods-publish";
        }
    }

    @GetMapping("/goods/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        System.out.println("测试查询缓存 - 第一次查询从数据库获取并缓存，第二次从缓存获取");
        Goods goods1 = goodsService.getGoodsByIdWithCache(id);
        if (goods1 == null){

            return "redirect:/goods";
        }
        model.addAttribute("goods", goods1);
        return "goods-edit";
        }

    @PostMapping("/goods/edit")
    public String editGoods(Goods goods,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                            @RequestParam(value = "removeImage", required = false) String removeImage,
                            Model model) {
        try {
            // 获取旧商品信息用于对比
            Goods oldGoods = goodsService.getGoodsById(goods.getId());
            
            System.out.println("=== 编辑商品调试信息 ===");
            System.out.println("商品ID: " + goods.getId());
            System.out.println("商品名称: " + goods.getName());
            System.out.println("当前状态: " + goods.getStatus());
            System.out.println("是否有新图片: " + (imageFile != null && !imageFile.isEmpty()));
            System.out.println("是否移除图片: " + removeImage);
            System.out.println("表单中的mainImage: [" + goods.getMainImage() + "]");
            if (oldGoods != null) {
                System.out.println("旧商品的mainImage: [" + oldGoods.getMainImage() + "]");
                System.out.println("旧商品的status: " + oldGoods.getStatus());
                System.out.println("旧商品的id: " + oldGoods.getId());
            } else {
                System.out.println("警告：无法获取旧商品信息！");
            }
            System.out.println("=========================");
            
            if (imageFile != null && !imageFile.isEmpty()) {
                // 上传了新图片，先删除旧图片
                System.out.println("【分支1】上传了新图片");
                if (oldGoods != null && oldGoods.getMainImage() != null) {
                    fileUploadUtil.deleteFile(oldGoods.getMainImage());
                }
                
                Files fileInfo = fileService.uploadFile(imageFile, "admin", "商品封面图");
                // 使用 /img/ 前缀匹配静态资源映射
                String imagePath = "/img/" + fileInfo.getFilePath();
                goods.setMainImage(imagePath);
                System.out.println("上传了新图片: " + imagePath);
            } else if ("true".equals(removeImage)) {
                // 用户主动移除了图片（只有这种情况下才删除）
                System.out.println("【分支2】用户主动移除图片，removeImage=" + removeImage);
                if (oldGoods != null && oldGoods.getMainImage() != null) {
                    fileUploadUtil.deleteFile(oldGoods.getMainImage());
                }
                goods.setMainImage(null);
                System.out.println("用户移除了图片");
            } else {
                // 没有上传新图片，也没有移除图片，保留原来的图片
                System.out.println("【分支3】保留原有图片");
                // 重要：无论状态如何变化，都要保留原有图片
                if (oldGoods != null && oldGoods.getMainImage() != null) {
                    goods.setMainImage(oldGoods.getMainImage());
                    System.out.println("保留原有图片: " + oldGoods.getMainImage());
                } else {
                    // 如果旧商品也没有图片，使用表单提交的mainImage值
                    System.out.println("警告：数据库中main_image字段为空！尝试使用表单中的值...");
                    if (goods.getMainImage() != null && !goods.getMainImage().isEmpty()) {
                        System.out.println("使用表单中的mainImage: " + goods.getMainImage());
                    } else {
                        System.out.println("错误：表单中也没有mainImage，设置为null");
                        goods.setMainImage(null);
                    }
                }
            }
            
            System.out.println("最终保存的mainImage: [" + goods.getMainImage() + "]");
            System.out.println("最终保存的状态: " + goods.getStatus());
            System.out.println("=========================");
            
            goodsService.updateGoods(goods);
            return "redirect:/";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "更新失败：" + e.getMessage());
            model.addAttribute("goods", goods);
            return "goods-edit";
        }
    }

    @PostMapping("/goods/delete/{id}")
    public String deleteGoods(@PathVariable Long id) {
        try {
            Goods goods = goodsService.getGoodsById(id);
            if (goods != null && goods.getMainImage() != null) {
                fileUploadUtil.deleteFile(goods.getMainImage());
            }
            goodsService.deleteGoods(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/";
    }

    @PostMapping("/goods/batch-delete")
    public String batchDeleteGoods(@RequestParam List<Long> ids) {
        try {
            for (Long id : ids) {
                Goods goods = goodsService.getGoodsById(id);
                if (goods != null && goods.getMainImage() != null) {
                    fileUploadUtil.deleteFile(goods.getMainImage());
                }
            }
            goodsService.batchDeleteGoods(ids);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/";
    }

    /**
     * 用户管理页面（仅管理员可访问）
     */
    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userService.list());
        return "admin/user-manage";
    }

    // ==================== 卖家专属API ====================

    /**
     * 卖家发布商品（自动关联当前登录的卖家ID）
     */
    @PostMapping("/seller/goods/publish")
    @ResponseBody
    public Map<String, Object> sellerPublishGoods(Goods goods,
                                                  @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                                  @RequestParam(value = "publishType", required = false, defaultValue = "now") String publishType,
                                                  @RequestParam(value = "scheduledTime", required = false) String scheduledTimeStr) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            String username = auth.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 检查是否为卖家角色
            if (!"SELLER".equals(currentUser.getRole())) {
                result.put("success", false);
                result.put("message", "只有卖家才能发布商品");
                return result;
            }

            // 设置卖家ID
            goods.setSellerId(currentUser.getId().longValue());

            // 处理图片上传
            if (imageFile != null && !imageFile.isEmpty()) {
                Files fileInfo = fileService.uploadFile(imageFile, username, "商品封面图");
                String imagePath = "/img/" + fileInfo.getFilePath();
                goods.setMainImage(imagePath);
            }

            // 处理定时发布逻辑
            if ("scheduled".equals(publishType) && scheduledTimeStr != null && !scheduledTimeStr.trim().isEmpty()) {
                try {
                    LocalDateTime scheduledTime = LocalDateTime.parse(scheduledTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    // 验证定时发布时间是否在当前时间之后（允许任意未来的时间）
                    LocalDateTime now = LocalDateTime.now();

                    if (scheduledTime.isBefore(now) || scheduledTime.isEqual(now)) {
                        result.put("success", false);
                        result.put("message", "定时发布时间必须晚于当前时间");
                        return result;
                    }

                    goods.setScheduledTime(scheduledTime);
                    goods.setStatus(0); // 定时发布时先设为"暂不上架"
                } catch (Exception e) {
                    System.err.println("解析定时发布时间失败: " + e.getMessage());
                    result.put("success", false);
                    result.put("message", "定时发布时间格式错误，请重新选择");
                    return result;
                }
            } else {
                goods.setScheduledTime(null); // 立即发布时不设置定时时间
            }

            // 设置默认值
            if (goods.getStatus() == null) {
                goods.setStatus(1); // 默认上架状态
            }
            if (goods.getStock() == null) {
                goods.setStock(0);
            }
            if (goods.getSalesCount() == null) {
                goods.setSalesCount(0);
            }

            goodsService.saveGoods(goods);

            // 构建成功消息
            String successMessage = "商品发布成功！";
            if (goods.getScheduledTime() != null) {
                successMessage = "商品已保存！将在 " + goods.getScheduledTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + " 自动上架";
            }

            result.put("success", true);
            result.put("message", successMessage);
            result.put("goodsId", goods.getId());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "发布失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 卖家编辑自己的商品（带权限验证）
     */
    @PostMapping("/seller/goods/update")
    @ResponseBody
    public Map<String, Object> sellerUpdateGoods(Goods goods,
                                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                                 @RequestParam(value = "removeImage", required = false) String removeImage) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            String username = auth.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 获取原商品信息
            Goods originalGoods = goodsService.getGoodsById(goods.getId());
            if (originalGoods == null) {
                result.put("success", false);
                result.put("message", "商品不存在");
                return result;
            }

            // 权限检查：只能编辑自己发布的商品
            if (!originalGoods.getSellerId().equals(currentUser.getId().longValue())) {
                result.put("success", false);
                result.put("message", "无权编辑此商品（只能编辑自己发布的商品）");
                return result;
            }

            // 处理图片更新
            if (imageFile != null && !imageFile.isEmpty()) {
                // 删除旧图片
                if (originalGoods.getMainImage() != null && !originalGoods.getMainImage().isEmpty()) {
                    fileUploadUtil.deleteFile(originalGoods.getMainImage());
                }
                Files fileInfo = fileService.uploadFile(imageFile, username, "商品封面图");
                String imagePath = "/img/" + fileInfo.getFilePath();
                goods.setMainImage(imagePath);
            } else if ("true".equals(removeImage)) {
                // 用户主动移除图片
                if (originalGoods.getMainImage() != null && !originalGoods.getMainImage().isEmpty()) {
                    fileUploadUtil.deleteFile(originalGoods.getMainImage());
                }
                goods.setMainImage(null);
            } else {
                // 保留原有图片
                goods.setMainImage(originalGoods.getMainImage());
            }

            // 保持卖家ID不变
            goods.setSellerId(currentUser.getId().longValue());

            goodsService.updateGoods(goods);

            result.put("success", true);
            result.put("message", "商品更新成功！");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 卖家删除自己的商品（带权限验证）
     */
    @PostMapping("/seller/goods/delete/{id}")
    @ResponseBody
    public Map<String, Object> sellerDeleteGoods(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            String username = auth.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 获取商品信息
            Goods goods = goodsService.getGoodsById(id);
            if (goods == null) {
                result.put("success", false);
                result.put("message", "商品不存在");
                return result;
            }

            // 权限检查：只能删除自己发布的商品
            if (!goods.getSellerId().equals(currentUser.getId().longValue())) {
                result.put("success", false);
                result.put("message", "无权删除此商品（只能删除自己发布的商品）");
                return result;
            }

            // 删除商品图片
            if (goods.getMainImage() != null && !goods.getMainImage().isEmpty()) {
                fileUploadUtil.deleteFile(goods.getMainImage());
            }

            // 删除商品记录
            goodsService.deleteGoods(id);

            result.put("success", true);
            result.put("message", "商品已成功删除！");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 卖家批量删除自己的商品（带权限验证）
     */
    @PostMapping("/seller/goods/batch-delete")
    @ResponseBody
    public Map<String, Object> sellerBatchDeleteGoods(@RequestBody List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            String username = auth.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            int deletedCount = 0;
            int forbiddenCount = 0;

            for (Long id : ids) {
                Goods goods = goodsService.getGoodsById(id);
                if (goods == null) continue;

                // 权限检查：只能删除自己发布的商品
                if (goods.getSellerId().equals(currentUser.getId().longValue())) {
                    // 删除商品图片
                    if (goods.getMainImage() != null && !goods.getMainImage().isEmpty()) {
                        fileUploadUtil.deleteFile(goods.getMainImage());
                    }
                    // 删除商品记录
                    goodsService.deleteGoods(id);
                    deletedCount++;
                } else {
                    forbiddenCount++;
                }
            }

            String message = "成功删除 " + deletedCount + " 个商品";
            if (forbiddenCount > 0) {
                message += "，" + forbiddenCount + " 个商品因不属于您而被跳过";
            }

            result.put("success", true);
            result.put("message", message);
            result.put("deletedCount", deletedCount);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "批量删除失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 卖家发货（标记订单为已发货状态）
     */
    @PostMapping("/seller/order/ship/{orderId}")
    @ResponseBody
    public Map<String, Object> shipOrder(@PathVariable Long orderId,
                                         @RequestParam(required = false) String trackingNumber,
                                         @RequestParam(required = false) String shippingCompany) {
        Map<String, Object> result = new HashMap<>();
        try {
            // TODO: 实现订单发货逻辑
            // 这里需要查询订单表，验证：
            // 1. 订单存在
            // 2. 订单中的商品属于当前卖家
            // 3. 订单状态为"待发货"
            // 然后更新订单状态为"已发货"，并记录物流信息

            result.put("success", true);
            result.put("message", "订单已发货！物流单号：" + (trackingNumber != null ? trackingNumber : "未填写"));
            result.put("trackingNumber", trackingNumber);
            result.put("shippingCompany", shippingCompany);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "发货失败：" + e.getMessage());
        }
        return result;
    }
}