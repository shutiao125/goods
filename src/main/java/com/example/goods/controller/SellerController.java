package com.example.goods.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.goods.Goods;
import com.example.goods.goods.GoodsResultMapVO;
import com.example.goods.goods.OrderItem;
import com.example.goods.goods.User;
import com.example.goods.mapper.GoodsMapper;
import com.example.goods.mapper.OrderItemMapper;
import com.example.goods.service.GoodsService;
import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/seller")
public class SellerController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.example.goods.service.OrderService orderService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 卖家仪表盘/主页
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null || !"SELLER".equals(currentUser.getRole())) {
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);

        try {
            Long sellerId = currentUser.getId().longValue();

            // 安全获取统计数据（每个统计独立try-catch，避免一个失败影响其他）
            int totalGoods = 0;
            int activeGoods = 0;
            int totalSales = 0;
            int pendingOrders = 0;

            try {
                totalGoods = goodsService.countBySellerId(sellerId);
            } catch (Exception e) {
                System.err.println("获取总商品数失败: " + e.getMessage());
            }

            try {
                activeGoods = goodsService.countActiveGoodsBySellerId(sellerId);
            } catch (Exception e) {
                System.err.println("获取上架商品数失败: " + e.getMessage());
            }

            try {
                totalSales = goodsService.getTotalSalesBySellerId(sellerId);
            } catch (Exception e) {
                System.err.println("获取总销量失败: " + e.getMessage());
            }

            // TODO: 从订单表查询待发货订单数
            pendingOrders = 0;

            model.addAttribute("totalGoods", totalGoods);
            model.addAttribute("activeGoods", activeGoods);
            model.addAttribute("totalSales", totalSales);
            model.addAttribute("pendingOrders", pendingOrders);

            // 安全获取最近商品
            List<GoodsResultMapVO> recentGoodsList = new java.util.ArrayList<>();
            try {
                Page<GoodsResultMapVO> recentGoods = goodsService.getSellerGoodsByPage(sellerId, 1, 5, null, null, null);
                if (recentGoods != null && recentGoods.getRecords() != null) {
                    recentGoodsList = recentGoods.getRecords();
                }
            } catch (Exception e) {
                System.err.println("获取最近商品失败: " + e.getMessage());
                e.printStackTrace();
            }

            model.addAttribute("recentGoods", recentGoodsList);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "加载数据失败：" + e.getMessage());
        }

        return "seller/dashboard";
    }

    /**
     * 商品管理页面
     */
    @GetMapping("/goods")
    public String goodsManage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null || !"SELLER".equals(currentUser.getRole())) {
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);

        try {
            Long sellerId = currentUser.getId().longValue();

            // 安全获取商品列表
            Page<GoodsResultMapVO> page = null;
            List<GoodsResultMapVO> goodsList = new java.util.ArrayList<>();
            int totalGoods = 0;

            try {
                page = goodsService.getSellerGoodsByPage(sellerId, pageNum, pageSize, keyword, null, null);
                if (page != null && page.getRecords() != null) {
                    goodsList = page.getRecords();
                }
            } catch (Exception e) {
                System.err.println("获取商品列表失败: " + e.getMessage());
                e.printStackTrace();
            }

            // 安全获取商品总数
            try {
                totalGoods = goodsService.countBySellerId(sellerId);
            } catch (Exception e) {
                System.err.println("获取商品总数失败: " + e.getMessage());
                totalGoods = 0;
            }

            model.addAttribute("goods", goodsList);
            model.addAttribute("page", page);
            model.addAttribute("keyword", keyword != null ? keyword : "");
            model.addAttribute("pageNum", pageNum);
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("totalGoods", totalGoods);

        } catch (Exception e) {
            System.err.println("商品管理页面加载失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "加载商品列表失败");
            model.addAttribute("goods", new java.util.ArrayList<>());
            model.addAttribute("page", null);
            model.addAttribute("totalGoods", 0);
        }

        return "seller/goods-manage";
    }

    /**
     * 发布商品页面
     */
    @GetMapping("/goods/publish")
    public String publishPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null || !"SELLER".equals(currentUser.getRole())) {
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("goods", new Goods());
        return "seller/goods-publish";
    }

    /**
     * 编辑商品页面
     */
    @GetMapping("/goods/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null || !"SELLER".equals(currentUser.getRole())) {
            return "redirect:/";
        }

        Goods goods = goodsService.getGoodsById(id);
        if (goods == null) {
            return "redirect:/seller/goods";
        }

        // 权限检查：只能编辑自己的商品
        if (!goods.getSellerId().equals(currentUser.getId().longValue())) {
            model.addAttribute("error", "无权编辑此商品");
            return "seller/goods-manage";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("goods", goods);
        return "seller/goods-edit";
    }

    /**
     * 订单管理页面（完全照搬管理后台功能）
     */
    @GetMapping("/orders")
    public String orderManage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null || !"SELLER".equals(currentUser.getRole())) {
            return "redirect:/";
        }

        model.addAttribute("currentUser", currentUser);

        try {
            Long sellerId = currentUser.getId().longValue();

            // 查询该卖家的订单列表
            Page<com.example.goods.goods.OrderInfo> orderPage = null;
            try {
                orderPage = orderService.getSellerOrders(sellerId, pageNum, pageSize, status, keyword);
            } catch (Exception e) {
                System.err.println("获取卖家订单失败: " + e.getMessage());
                e.printStackTrace();
                orderPage = new Page<>(pageNum, pageSize);
            }

            model.addAttribute("orderPage", orderPage);
            model.addAttribute("status", status);
            model.addAttribute("keyword", keyword != null ? keyword : "");

        } catch (Exception e) {
            System.err.println("订单管理页面加载失败: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "加载订单列表失败");
            model.addAttribute("orderPage", new Page<>(pageNum, pageSize));
            model.addAttribute("status", null);
            model.addAttribute("keyword", "");
        }

        return "seller/order-manage";
    }

    /**
     * 卖家发货（卖家专属API，避免CSRF问题）
     */
    @PostMapping("/orders/ship")
    @ResponseBody
    public Map<String, Object> sellerShipOrder(
            @RequestParam Long orderId,
            @RequestParam String expressCompany,
            @RequestParam String expressNo) {
        Map<String, Object> result = new HashMap<>();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            String username = auth.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null || !"SELLER".equals(currentUser.getRole())) {
                result.put("success", false);
                result.put("message", "无权限操作");
                return result;
            }

            Long sellerId = currentUser.getId().longValue();

            com.example.goods.goods.OrderInfo order = orderService.getById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            if (order.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "订单状态不允许发货（当前状态：" + getOrderStatusText(order.getStatus()) + "）");
                return result;
            }

            boolean isSellerOrder = checkIfSellerOrder(sellerId, orderId);
            if (!isSellerOrder) {
                result.put("success", false);
                result.put("message", "无权操作此订单（该订单不包含您的商品）");
                return result;
            }

            boolean shipped = orderService.shipOrder(orderId, expressCompany, expressNo);
            if (shipped) {
                System.out.println("✅ [卖家后台] 发货成功 - 订单ID: " + orderId +
                        ", 卖家: " + username +
                        ", 物流: " + expressCompany +
                        ", 单号: " + expressNo);

                result.put("success", true);
                result.put("message", "发货成功！物流公司：" + expressCompany + "，快递单号：" + expressNo);
            } else {
                result.put("success", false);
                result.put("message", "发货失败，请重试");
            }
        } catch (Exception e) {
            System.err.println("❌ [卖家后台] 发货失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "发货失败：" + e.getMessage());
        }

        return result;
    }

    private boolean checkIfSellerOrder(Long sellerId, Long orderId) {
        try {
            LambdaQueryWrapper<Goods> goodsWrapper = new LambdaQueryWrapper<>();
            goodsWrapper.eq(Goods::getSellerId, sellerId)
                       .select(Goods::getId);
            List<Goods> sellerGoods = goodsMapper.selectList(goodsWrapper);

            if (sellerGoods == null || sellerGoods.isEmpty()) {
                return false;
            }

            List<Long> goodsIds = new ArrayList<>();
            for (Goods g : sellerGoods) {
                if (g.getId() != null) {
                    goodsIds.add(g.getId());
                }
            }

            if (goodsIds.isEmpty()) {
                return false;
            }

            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(OrderItem::getGoodsId, goodsIds)
                      .eq(OrderItem::getOrderId, orderId);
            Long count = orderItemMapper.selectCount(itemWrapper);

            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("检查订单归属失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 卖家完成订单（卖家专属API）
     */
    @PostMapping("/orders/{id}/complete")
    @ResponseBody
    public Map<String, Object> sellerCompleteOrder(@PathVariable("id") Long orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            String username = auth.getName();
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null || !"SELLER".equals(currentUser.getRole())) {
                result.put("success", false);
                result.put("message", "无权限操作");
                return result;
            }

            Long sellerId = currentUser.getId().longValue();

            com.example.goods.goods.OrderInfo order = orderService.getById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            if (order.getStatus() != 2) {
                result.put("success", false);
                result.put("message", "订单状态不允许完成（当前状态：" + getOrderStatusText(order.getStatus()) + "）");
                return result;
            }

            boolean isSellerOrder = checkIfSellerOrder(sellerId, orderId);
            if (!isSellerOrder) {
                result.put("success", false);
                result.put("message", "无权操作此订单（该订单不包含您的商品）");
                return result;
            }

            boolean completed = orderService.completeOrder(orderId);
            if (completed) {
                System.out.println("✅ [卖家后台] 完成订单成功 - 订单ID: " + orderId +
                        ", 卖家: " + username);

                result.put("success", true);
                result.put("message", "订单已完成！");
            } else {
                result.put("success", false);
                result.put("message", "完成订单失败，请重试");
            }
        } catch (Exception e) {
            System.err.println("❌ [卖家后台] 完成订单失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "完成订单失败：" + e.getMessage());
        }

        return result;
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待付款";
            case 1: return "待发货";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知状态";
        }
    }
}