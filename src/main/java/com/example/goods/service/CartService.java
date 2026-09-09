package com.example.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goods.goods.Cart;

import java.util.List;

public interface CartService extends IService<Cart> {

    List<Cart> getCartByUserId(Long userId);

    boolean addToCart(Long userId, Long goodsId, Integer quantity);

    boolean updateCartQuantity(Long cartId, Long userId, Integer quantity);

    boolean removeFromCart(Long cartId, Long userId);

    boolean clearCart(Long userId);
}
