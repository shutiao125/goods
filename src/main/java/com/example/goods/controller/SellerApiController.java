package com.example.goods.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.common.Result;
import com.example.goods.goods.*;
import com.example.goods.mapper.GoodsMapper;
import com.example.goods.service.GoodsService;
import com.example.goods.service.OrderService;
import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/seller")
public class SellerApiController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private GoodsMapper goodsMapper;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        String username = auth.getName();
        User user = userService.getUserByUsername(username);
        return user != null ? user.getId().longValue() : null;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getSellerStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            Long sellerId = getCurrentUserId();
            if (sellerId == null) {
                return Result.error("未登录");
            }

            stats.put("totalGoods", goodsService.countBySellerId(sellerId));
            stats.put("activeGoods", goodsService.countActiveGoodsBySellerId(sellerId));
            stats.put("totalSales", goodsService.getTotalSalesBySellerId(sellerId));
            stats.put("pendingOrders", getPendingOrdersCount(sellerId));

            LambdaQueryWrapper<OrderInfo> incomeWrapper = new LambdaQueryWrapper<>();
            incomeWrapper.eq(OrderInfo::getStatus, 3);
            List<OrderInfo> completedOrders = orderService.list(incomeWrapper);

            double totalRevenue = 0;
            if (completedOrders != null) {
                for (OrderInfo order : completedOrders) {
                    if (order.getPayAmount() != null) {
                        totalRevenue += order.getPayAmount().doubleValue();
                    }
                }
            }
            stats.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败");
        }

        return Result.success(stats);
    }

    private long getPendingOrdersCount(Long sellerId) {
        try {
            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(OrderInfo::getStatus, 0, 1);
            return orderService.count(wrapper);
        } catch (Exception e) {
            return 0;
        }
    }

    @GetMapping("/orders/recent")
    public Result<List<OrderInfo>> getRecentOrders() {
        try {
            Long sellerId = getCurrentUserId();
            if (sellerId == null) {
                return Result.error("未登录");
            }

            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(OrderInfo::getCreatedAt);
            wrapper.last("LIMIT 10");

            List<OrderInfo> orders = orderService.list(wrapper);
            return Result.success(orders);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取订单失败");
        }
    }

    @GetMapping("/goods/top")
    public Result<List<Goods>> getTopGoods() {
        try {
            Long sellerId = getCurrentUserId();
            if (sellerId == null) {
                return Result.error("未登录");
            }

            LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Goods::getSellerId, sellerId.intValue());
            wrapper.eq(Goods::getStatus, 1);
            wrapper.orderByDesc(Goods::getSalesCount);
            wrapper.last("LIMIT 10");

            List<Goods> topGoods = goodsMapper.selectList(wrapper);
            return Result.success(topGoods);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取热销商品失败");
        }
    }

    @GetMapping("/goods/page")
    public Result<Page<Goods>> getSellerGoodsPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        try {
            Long sellerId = getCurrentUserId();
            if (sellerId == null) {
                return Result.error("未登录");
            }

            Page<Goods> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();

            wrapper.eq(Goods::getSellerId, sellerId.intValue());

            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.like(Goods::getName, keyword);
            }

            wrapper.orderByDesc(Goods::getCreatedAt);

            Page<Goods> result = goodsMapper.selectPage(page, wrapper);
            return Result.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取商品列表失败");
        }
    }

    @PutMapping("/goods/{id}/status")
    public Result<String> updateGoodsStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Long sellerId = getCurrentUserId();
            if (sellerId == null) {
                return Result.error("未登录");
            }

            Goods goods = goodsMapper.selectById(id);
            if (goods == null) {
                return Result.error("商品不存在");
            }

            if (!goods.getSellerId().equals(sellerId.intValue())) {
                return Result.error("无权操作此商品");
            }

            Integer status = body.get("status");
            goods.setStatus(status);
            goodsMapper.updateById(goods);

            return Result.success(status == 1 ? "上架成功" : "下架成功");

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新状态失败");
        }
    }

    @DeleteMapping("/goods/{id}")
    public Result<String> deleteGoods(@PathVariable Long id) {
        try {
            Long sellerId = getCurrentUserId();
            if (sellerId == null) {
                return Result.error("未登录");
            }

            Goods goods = goodsMapper.selectById(id);
            if (goods == null) {
                return Result.error("商品不存在");
            }

            if (!goods.getSellerId().equals(sellerId.intValue())) {
                return Result.error("无权删除此商品");
            }

            int result = goodsMapper.deleteById(id);
            if (result > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }
}