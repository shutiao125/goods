package com.example.goods.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.goods.*;
import com.example.goods.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private com.example.goods.mapper.OrderItemMapper orderItemMapper;

    @GetMapping("")
    public String orderList(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            @RequestParam(required = false) Integer status,
                            Model model) {
        Long userId = getCurrentUserId();
        Page<OrderInfo> orderPage = orderService.getOrdersByUserId(userId, pageNum, pageSize, status);

        // 为每个订单查询商品明细
        if (orderPage != null && orderPage.getRecords() != null) {
            for (OrderInfo order : orderPage.getRecords()) {
                List<OrderItem> items = getOrderItems(order.getId());
                order.setItems(items);  // 需要在OrderInfo中添加items字段
            }
        }

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("orders", orderPage.getRecords());
        model.addAttribute("status", status);
        return "admin/order-list";
    }

    @GetMapping("/create")
    public String createOrderPage(@RequestParam List<Long> cartIds, Model model) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return "redirect:/login";
            }

            List<Address> addresses = addressService.getAddressByUserId(userId);
            model.addAttribute("addresses", addresses != null ? addresses : new java.util.ArrayList<>());
            model.addAttribute("cartIds", cartIds);
            model.addAttribute("defaultAddress", addressService.getDefaultAddress(userId));

            // 查询购物车商品信息并传递给前端
            try {
                System.out.println("\n========== 订单页面调试信息 ==========");
                System.out.println("🛒 [OrderController] 开始查询购物车数据...");
                System.out.println("🛒 [OrderController] userId: " + userId);
                System.out.println("🛒 [OrderController] cartIds: " + cartIds);

                List<Cart> cartItems = cartService.getCartByUserId(userId);
                System.out.println("🛒 [OrderController] 查询到购物车商品数: " +
                    (cartItems != null ? cartItems.size() : 0));

                if (cartItems != null && !cartItems.isEmpty()) {
                    for (Cart c : cartItems) {
                        System.out.println("  📦 购物车项: id=" + c.getId() +
                            ", goodsId=" + c.getGoodsId() +
                            ", quantity=" + c.getQuantity());
                    }
                }

                List<Map<String, Object>> cartWithGoods = new java.util.ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;

                if (cartItems != null) {
                    for (Cart cart : cartItems) {
                        System.out.println("🛒 [OrderController] 处理购物车项: id=" + cart.getId() +
                            ", goodsId=" + cart.getGoodsId() + ", quantity=" + cart.getQuantity());

                        // 如果指定了 cartIds，只处理选中的商品
                        if (cartIds != null && !cartIds.isEmpty() && !cartIds.contains(cart.getId())) {
                            System.out.println("  ⏭️ 跳过未选中的商品: id=" + cart.getId() +
                                " (不在cartIds列表中: " + cartIds + ")");
                            continue;
                        }

                        Goods goods = goodsService.getGoodsById(cart.getGoodsId());
                        if (goods != null) {
                            System.out.println("  ✅ 找到商品: " + goods.getName() +
                                ", 价格: " + goods.getPrice() + ", 状态: " + goods.getStatus());

                            if (goods.getStatus() == 1) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("cartId", cart.getId());
                                item.put("goodsId", goods.getId());
                                item.put("goodsName", goods.getName());
                                item.put("goodsImage", goods.getMainImage());
                                item.put("price", goods.getPrice());
                                item.put("quantity", cart.getQuantity());

                                BigDecimal subtotal = goods.getPrice().multiply(new BigDecimal(cart.getQuantity()));
                                item.put("subtotal", subtotal);

                                totalAmount = totalAmount.add(subtotal);
                                cartWithGoods.add(item);

                                System.out.println("  ✅✅ 添加到订单: 小计=" + subtotal);
                            } else {
                                System.out.println("  ❌ 商品已下架: status=" + goods.getStatus());
                            }
                        } else {
                            System.out.println("  ❌ 未找到商品: goodsId=" + cart.getGoodsId());
                        }
                    }
                }

                System.out.println("✅ [OrderController] 最终商品数: " + cartWithGoods.size() +
                    ", 总金额: " + totalAmount);
                System.out.println("========================================\n");

                model.addAttribute("cartItems", cartWithGoods);
                model.addAttribute("totalAmount", totalAmount);

                // 添加调试信息到model（前端可以显示）
                model.addAttribute("debugInfo", Map.of(
                    "userId", userId,
                    "cartIdsParam", cartIds,
                    "allCartItemsCount", cartItems != null ? cartItems.size() : 0,
                    "filteredCartItemsCount", cartWithGoods.size(),
                    "totalAmount", totalAmount
                ));
            } catch (Exception e) {
                System.err.println("❌ [OrderController] 加载购物车数据失败: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("cartError", "加载购物车数据失败: " + e.getMessage());
            }

            return "admin/order-create";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "加载订单页面失败：" + e.getMessage());
            return "redirect:/cart?error=1";
        }
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam List<Long> cartIds,
                              @RequestParam Long addressId,
                              @RequestParam(required = false) String remark,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            System.out.println("\n========== 创建订单 ==========");
            System.out.println("📦 [OrderController] 开始创建订单...");
            System.out.println("🛒 [OrderController] cartIds: " + cartIds);
            System.out.println("📍 [OrderController] addressId: " + addressId);
            System.out.println("📝 [OrderController] remark: " + remark);

            Long userId = getCurrentUserId();
            System.out.println("👤 [OrderController] userId: " + userId);

            if (userId == null) {
                throw new Exception("用户未登录");
            }

            if (addressId == null || addressId <= 0) {
                throw new Exception("请选择收货地址");
            }

            if (cartIds == null || cartIds.isEmpty()) {
                throw new Exception("购物车为空");
            }

            OrderInfo order = orderService.createOrder(userId, cartIds, addressId, remark);

            System.out.println("✅ [OrderController] 订单创建成功! orderId: " + order.getId());
            System.out.println("==============================\n");

            return "redirect:/order/" + order.getId() + "/pay";
        } catch (Exception e) {
            System.err.println("❌ [OrderController] 创建订单失败: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==============================\n");

            redirectAttributes.addFlashAttribute("error", "订单创建失败: " + e.getMessage());
            return "redirect:/order/create?cartIds=" + String.join(",", cartIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList()));
        }
    }

    @GetMapping("/{id}/pay")
    public String payPage(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("\n========== 订单支付页面 ==========");
            System.out.println("💳 [OrderController] 开始加载支付页面...");
            System.out.println("📦 [OrderController] orderId: " + id);

            Long userId = getCurrentUserId();
            System.out.println("👤 [OrderController] userId: " + userId);

            if (userId == null) {
                System.err.println("❌ [OrderController] 用户未登录");
                return "redirect:/login";
            }

            OrderInfo order = orderService.getOrderDetail(id, userId);
            if (order == null) {
                System.err.println("❌ [OrderController] 订单不存在或不属于当前用户");
                redirectAttributes.addFlashAttribute("error", "订单不存在或无权访问");
                return "redirect:/order";
            }

            System.out.println("📋 [OrderController] 订单信息:");
            System.out.println("   - orderNo: " + order.getOrderNo());
            System.out.println("   - payAmount: " + order.getPayAmount());
            System.out.println("   - status: " + order.getStatus());

            List<OrderItem> items = getOrderItems(order.getId());
            System.out.println("🛒 [OrderController] 商品数量: " + items.size());

            model.addAttribute("order", order);
            model.addAttribute("items", items);

            System.out.println("✅ [OrderController] 支付页面加载成功");
            System.out.println("==============================\n");

            return "admin/order-pay";  // ✅ 修正模板路径
        } catch (Exception e) {
            System.err.println("❌ [OrderController] 加载支付页面失败:");
            e.printStackTrace();
            System.err.println("==============================\n");

            redirectAttributes.addFlashAttribute("error", "加载支付页面失败: " + e.getMessage());
            return "redirect:/order";
        }
    }

    @PostMapping("/{id}/pay")
    @ResponseBody
    public Map<String, Object> payOrder(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            boolean success = orderService.payOrder(id, userId);
            result.put("success", success);
            result.put("message", success ? "支付成功" : "支付失败");
            result.put("orderId", id);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Long userId = getCurrentUserId();
        OrderInfo order = orderService.getOrderDetail(id, userId);
        if (order == null) {
            return "redirect:/order";
        }

        List<OrderItem> items = getOrderItems(order.getId());
        model.addAttribute("order", order);
        model.addAttribute("items", items);

        return "admin/order-detail";  // ✅ 修正模板路径
    }

    @PostMapping("/{id}/cancel")
    @ResponseBody
    public Map<String, Object> cancelOrder(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            boolean success = orderService.cancelOrder(id, userId);
            result.put("success", success);
            result.put("message", success ? "取消成功" : "取消失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/{id}/confirm")
    @ResponseBody
    public Map<String, Object> confirmReceive(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            OrderInfo order = orderService.getOrderDetail(id, userId);
            if (order != null && order.getStatus() == 2) {
                boolean success = orderService.completeOrder(id);
                result.put("success", success);
                result.put("message", success ? "确认收货成功" : "确认收货失败");
            } else {
                result.put("success", false);
                result.put("message", "订单状态不正确");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private List<OrderItem> getOrderItems(Long orderId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getUserByUsername(username);
        return user != null ? user.getId() : null;
    }
}