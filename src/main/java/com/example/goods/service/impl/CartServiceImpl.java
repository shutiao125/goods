package com.example.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goods.goods.Cart;
import com.example.goods.goods.Goods;
import com.example.goods.mapper.CartMapper;
import com.example.goods.mapper.GoodsMapper;
import com.example.goods.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public List<Cart> getCartByUserId(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreatedAt);
        return cartMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public boolean addToCart(Long userId, Long goodsId, Integer quantity) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() != 1) {
            return false;
        }

        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
                .eq(Cart::getGoodsId, goodsId);

        Cart existingCart = cartMapper.selectOne(wrapper);

        if (existingCart != null) {
            existingCart.setQuantity(existingCart.getQuantity() + quantity);
            return cartMapper.updateById(existingCart) > 0;
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setGoodsId(goodsId);
            cart.setQuantity(quantity);
            return cartMapper.insert(cart) > 0;
        }
    }

    @Override
    @Transactional
    public boolean updateCartQuantity(Long cartId, Long userId, Integer quantity) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getId, cartId)
                .eq(Cart::getUserId, userId)
                .set(Cart::getQuantity, quantity);
        return cartMapper.update(null, wrapper) > 0;
    }

    @Override
    @Transactional
    public boolean removeFromCart(Long cartId, Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getId, cartId)
                .eq(Cart::getUserId, userId);
        return cartMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional
    public boolean clearCart(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        return cartMapper.delete(wrapper) >= 0;
    }
}
