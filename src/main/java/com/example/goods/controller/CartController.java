package com.example.goods.controller;

import com.example.goods.goods.Cart;
import com.example.goods.goods.Goods;
import com.example.goods.goods.User;
import com.example.goods.service.CartService;
import com.example.goods.service.GoodsService;
import com.example.goods.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private UserService userService;

    @GetMapping("")
    public String cartPage(Model model) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return "redirect:/login";
        }

        List<Cart> cartItems = cartService.getCartByUserId(userId);
        if (cartItems == null) {
            cartItems = new java.util.ArrayList<>();
        }

        List<Map<String, Object>> cartWithGoods = new java.util.ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;

        for (Cart cart : cartItems) {
            Goods goods = goodsService.getGoodsById(cart.getGoodsId());
            if (goods != null && goods.getStatus() == 1) {
                Map<String, Object> item = new HashMap<>();
                item.put("cart", cart);
                item.put("goods", goods);

                BigDecimal subtotal = goods.getPrice().multiply(new java.math.BigDecimal(cart.getQuantity()));
                item.put("subtotal", subtotal);

                totalAmount = totalAmount.add(subtotal);
                totalCount += cart.getQuantity();

                cartWithGoods.add(item);
            }
        }

        model.addAttribute("cartItems", cartWithGoods);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("totalCount", totalCount);

        return "admin/cart";
    }

    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> addToCart(@RequestParam Long goodsId,
                                         @RequestParam(defaultValue = "1") Integer quantity) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            boolean success = cartService.addToCart(userId, goodsId, quantity);

            if (success) {
                // 查询刚添加或更新的购物车项ID
                Long cartId = null;
                try {
                    List<Cart> cartItems = cartService.getCartByUserId(userId);
                    if (cartItems != null && !cartItems.isEmpty()) {
                        // 查找该商品的购物车项
                        for (Cart cart : cartItems) {
                            if (cart.getGoodsId().equals(goodsId)) {
                                cartId = cart.getId();
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ [CartController] 查询cartId失败: " + e.getMessage());
                }

                result.put("success", true);
                result.put("message", "添加成功");
                result.put("cartId", cartId);
                System.out.println("✅ [CartController] 添加购物车成功: goodsId=" + goodsId + ", cartId=" + cartId);
            } else {
                result.put("success", false);
                result.put("message", "添加失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            System.err.println("❌ [CartController] 添加购物车异常: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update/{id}")
    @ResponseBody
    public Map<String, Object> updateCart(@PathVariable Long id,
                                          @RequestParam Integer quantity) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            boolean success = cartService.updateCartQuantity(id, userId, quantity);
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteCart(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            boolean success = cartService.removeFromCart(id, userId);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/clear")
    @ResponseBody
    public Map<String, Object> clearCart() {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            boolean success = cartService.clearCart(userId);
            result.put("success", success);
            result.put("message", success ? "清空成功" : "清空失败");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/count")
    @ResponseBody
    public Map<String, Object> getCartCount() {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = getCurrentUserId();
            List<Cart> cartItems = cartService.getCartByUserId(userId);
            int count = cartItems.stream().mapToInt(Cart::getQuantity).sum();
            result.put("success", true);
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("count", 0);
        }
        return result;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getUserByUsername(username);
        return user != null ? user.getId() : null;
    }
}