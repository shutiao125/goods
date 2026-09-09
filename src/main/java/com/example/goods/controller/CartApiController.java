package com.example.goods.controller;

import com.example.goods.common.Result;
import com.example.goods.goods.Cart;
import com.example.goods.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        // 这里需要从 UserService 获取用户ID，暂时使用固定值或从 token 中获取
        return 1L; // TODO: 实际应该从认证信息中获取用户ID
    }

    @GetMapping("/count")
    public Result<Integer> getCartCount() {
        try {
            Long userId = getCurrentUserId();
            List<Cart> cartItems = cartService.getCartByUserId(userId);
            int count = cartItems != null ? cartItems.size() : 0;
            return Result.success(count);
        } catch (Exception e) {
            return Result.success(0);
        }
    }

    @GetMapping
    public Result<List<Cart>> getCartItems() {
        Long userId = getCurrentUserId();
        List<Cart> cartItems = cartService.getCartByUserId(userId);
        return Result.success(cartItems);
    }

    @PostMapping
    public Result<String> addToCart(@RequestBody Cart cartRequest) {
        Long userId = getCurrentUserId();
        boolean success = cartService.addToCart(userId, cartRequest.getGoodsId(), 1);
        if (success) {
            return Result.success("添加到购物车成功", "添加成功");
        }
        return Result.error("添加到购物车失败");
    }

    @PutMapping("/{id}")
    public Result<String> updateCartItem(@PathVariable Long id, @RequestParam Integer quantity) {
        Long userId = getCurrentUserId();
        boolean success = cartService.updateCartQuantity(id, userId, quantity);
        if (success) {
            return Result.success("购物车项更新成功", "更新成功");
        }
        return Result.error("购物车项更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> removeFromCart(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean success = cartService.removeFromCart(id, userId);
        if (success) {
            return Result.success("从购物车移除成功", "移除成功");
        }
        return Result.error("从购物车移除失败");
    }

    @DeleteMapping("/clear")
    public Result<String> clearCart() {
        Long userId = getCurrentUserId();
        boolean success = cartService.clearCart(userId);
        if (success) {
            return Result.success("清空购物车成功", "清空成功");
        }
        return Result.error("清空购物车失败");
    }
}