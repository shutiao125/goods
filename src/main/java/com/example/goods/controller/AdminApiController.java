package com.example.goods.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.common.Result;
import com.example.goods.goods.*;
import com.example.goods.mapper.GoodsMapper;
import com.example.goods.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private GoodsMapper goodsMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalUsers", userService.count());
            stats.put("totalGoods", goodsService.count());
            stats.put("totalOrders", orderService.count());
            stats.put("pendingOrders", getPendingOrdersCount());
            stats.put("todayNewUsers", getTodayNewUsersCount());
            stats.put("todayNewGoods", getTodayNewGoodsCount());
            stats.put("lowStockCount", getLowStockCount());

            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

            LambdaQueryWrapper<OrderInfo> incomeWrapper = new LambdaQueryWrapper<>();
            incomeWrapper.ge(OrderInfo::getCreatedAt, startOfDay);
            incomeWrapper.lt(OrderInfo::getCreatedAt, endOfDay);
            incomeWrapper.eq(OrderInfo::getStatus, 3);
            List<OrderInfo> completedOrders = orderService.list(incomeWrapper);

            double totalSales = 0;
            if (completedOrders != null) {
                for (OrderInfo order : completedOrders) {
                    if (order.getPayAmount() != null) {
                        totalSales += order.getPayAmount().doubleValue();
                    }
                }
            }
            stats.put("totalSales", Math.round(totalSales * 100.0) / 100.0);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败");
        }

        return Result.success(stats);
    }

    private long getPendingOrdersCount() {
        try {
            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(OrderInfo::getStatus, 0, 1);
            return orderService.count(wrapper);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getTodayNewUsersCount() {
        try {
            LocalDate today = LocalDate.now();
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getCreatedAt, today.atStartOfDay());
            return (int) userService.count(wrapper);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getTodayNewGoodsCount() {
        try {
            LocalDate today = LocalDate.now();
            LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Goods::getCreatedAt, today.atStartOfDay());
            return goodsMapper.selectCount(wrapper).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getLowStockCount() {
        try {
            LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
            wrapper.le(Goods::getStock, 10);
            wrapper.eq(Goods::getStatus, 1);
            return goodsMapper.selectCount(wrapper).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @GetMapping("/activities")
    public Result<List<Map<String, String>>> getRecentActivities() {
        List<Map<String, String>> activities = new ArrayList<>();

        try {
            activities.add(createActivity(LocalDateTime.now().toString(), "系统启动完成"));
            activities.add(createActivity(LocalDateTime.now().minusHours(1).toString(), "新增用户注册: user_" + System.currentTimeMillis() % 10000));
            activities.add(createActivity(LocalDateTime.now().minusHours(2).toString(), "新商品发布: 商品 #" + (int)(Math.random() * 1000)));
            activities.add(createActivity(LocalDateTime.now().minusHours(3).toString(), "订单创建成功: 订单 #" + (int)(Math.random() * 500)));
            activities.add(createActivity(LocalDateTime.now().minusHours(5).toString(), "用户完成支付: ¥" + (int)(Math.random() * 1000)));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.success(activities);
    }

    private Map<String, String> createActivity(String time, String content) {
        Map<String, String> activity = new HashMap<>();
        activity.put("time", time.substring(0, 19).replace("T", " "));
        activity.put("content", content);
        return activity;
    }

    @GetMapping("/goods/page")
    public Result<Page<Goods>> getGoodsPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {

        try {
            Page<Goods> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like(Goods::getName, keyword)
                        .or()
                        .like(Goods::getDescription, keyword));
            }

            if (categoryId != null) {
                wrapper.eq(Goods::getCategoryId, categoryId);
            }

            if (status != null) {
                wrapper.eq(Goods::getStatus, status);
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
            Goods goods = goodsMapper.selectById(id);
            if (goods == null) {
                return Result.error("商品不存在");
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