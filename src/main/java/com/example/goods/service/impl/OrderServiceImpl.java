package com.example.goods.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goods.goods.*;
import com.example.goods.mapper.*;
import com.example.goods.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Override
    @Transactional
    public OrderInfo createOrder(Long userId, List<Long> cartIds, Long addressId, String remark) {
        if (cartIds == null || cartIds.isEmpty()) {
            throw new RuntimeException("购物车不能为空");
        }

        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new RuntimeException("收货地址无效");
        }

        List<Cart> cartItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Long cartId : cartIds) {
            Cart cart = cartMapper.selectById(cartId);
            if (cart != null && cart.getUserId().equals(userId)) {
                Goods goods = goodsMapper.selectById(cart.getGoodsId());
                if (goods != null && goods.getStatus() == 1 && goods.getStock() >= cart.getQuantity()) {
                    cartItems.add(cart);
                    totalAmount = totalAmount.add(goods.getPrice().multiply(new BigDecimal(cart.getQuantity())));
                }
            }
        }

        if (cartItems.isEmpty()) {
            throw new RuntimeException("没有有效的商品");
        }

        String orderNo = generateOrderNo();

        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(0); // 待付款
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getDetailAddress());
        order.setPayType(1); // 模拟支付
        order.setRemark(remark);
        order.setCreatedAt(LocalDateTime.now());

        orderMapper.insert(order);

        for (Cart cart : cartItems) {
            Goods goods = goodsMapper.selectById(cart.getGoodsId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setGoodsId(goods.getId());
            orderItem.setGoodsName(goods.getName());
            orderItem.setGoodsImage(goods.getMainImage());
            orderItem.setGoodsPrice(goods.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setSubtotal(goods.getPrice().multiply(new BigDecimal(cart.getQuantity())));

            orderItemMapper.insert(orderItem);

            goods.setStock(goods.getStock() - cart.getQuantity());
            goods.setSalesCount(goods.getSalesCount() + cart.getQuantity());
            goodsMapper.updateById(goods);

            cartMapper.deleteById(cart.getId());
        }

        return order;
    }

    @Override
    @Transactional
    public boolean payOrder(Long orderId, Long userId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return false;
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setStatus(1); // 待发货
        order.setPayTime(LocalDateTime.now());
        return orderMapper.updateById(order) > 0;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long orderId, Long userId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return false;
        }

        // 允许取消"待付款(0)"和"待发货(1)"的订单
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new RuntimeException("只能取消待付款或待发货的订单");
        }

        order.setStatus(4); // 已取消
        boolean result = orderMapper.updateById(order) > 0;

        if (result) {
            LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderItem::getOrderId, orderId);
            List<OrderItem> items = orderItemMapper.selectList(wrapper);

            for (OrderItem item : items) {
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods != null) {
                    goods.setStock(goods.getStock() + item.getQuantity());
                    goods.setSalesCount(goods.getSalesCount() - item.getQuantity());
                    goodsMapper.updateById(goods);
                }
            }
        }

        return result;
    }

    @Override
    @Transactional
    public boolean adminCancelOrder(Long orderId) {
        System.out.println("🗑️ [OrderServiceImpl] 管理员取消订单开始, orderId: " + orderId);

        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null) {
            System.err.println("❌ [OrderServiceImpl] 订单不存在: " + orderId);
            return false;
        }

        System.out.println("📋 [OrderServiceImpl] 订单信息:");
        System.out.println("   - orderId: " + order.getId());
        System.out.println("   - orderNo: " + order.getOrderNo());
        System.out.println("   - userId: " + order.getUserId());
        System.out.println("   - status: " + order.getStatus());

        // 允许取消"待付款(0)"和"待发货(1)"的订单（不检查用户归属）
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            String errorMsg = "只能取消待付款或待发货的订单(当前状态:" + order.getStatus() + ")";
            System.err.println("❌ [OrderServiceImpl] " + errorMsg);
            throw new RuntimeException(errorMsg);
        }

        order.setStatus(4); // 已取消
        boolean result = orderMapper.updateById(order) > 0;

        if (result) {
            System.out.println("✅ [OrderServiceImpl] 订单状态已更新为已取消");

            LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderItem::getOrderId, orderId);
            List<OrderItem> items = orderItemMapper.selectList(wrapper);

            System.out.println("🛒 [OrderServiceImpl] 订单商品数: " + items.size());

            int restoredCount = 0;
            for (OrderItem item : items) {
                Goods goods = goodsMapper.selectById(item.getGoodsId());
                if (goods != null) {
                    int oldStock = goods.getStock();
                    int oldSales = goods.getSalesCount();

                    goods.setStock(goods.getStock() + item.getQuantity());
                    goods.setSalesCount(goods.getSalesCount() - item.getQuantity());
                    goodsMapper.updateById(goods);

                    restoredCount++;
                    System.out.println("  📦 恢复库存: " + goods.getName()
                            + " 库存: " + oldStock + " → " + goods.getStock()
                            + ", 销量: " + oldSales + " → " + goods.getSalesCount());
                }
            }

            System.out.println("✅ [OrderServiceImpl] 成功恢复 " + restoredCount + " 个商品的库存");
        } else {
            System.err.println("❌ [OrderServiceImpl] 更新订单状态失败");
        }

        System.out.println("✅ [OrderServiceImpl] 管理员取消订单完成\n");
        return result;
    }

    @Override
    @Transactional
    public boolean shipOrder(Long orderId, String expressCompany, String expressNo) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("只能对待发货的订单进行发货操作");
        }

        order.setStatus(2); // 已发货
        order.setExpressCompany(expressCompany);
        order.setExpressNo(expressNo);
        order.setDeliveryTime(LocalDateTime.now());
        return orderMapper.updateById(order) > 0;
    }

    @Override
    @Transactional
    public boolean completeOrder(Long orderId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() != 2) {
            throw new RuntimeException("只能对已发货的订单进行确认收货");
        }

        order.setStatus(3); // 已完成
        order.setCompleteTime(LocalDateTime.now());
        return orderMapper.updateById(order) > 0;
    }

    @Override
    public Page<OrderInfo> getOrdersByUserId(Long userId, int pageNum, int pageSize, Integer status) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId)
                .eq(status != null, OrderInfo::getStatus, status)
                .orderByDesc(OrderInfo::getCreatedAt);
        return orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Page<OrderInfo> getAllOrders(int pageNum, int pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, OrderInfo::getStatus, status)
                .like(StrUtil.isNotBlank(keyword), OrderInfo::getOrderNo, keyword)
                .or()
                .like(StrUtil.isNotBlank(keyword), OrderInfo::getReceiverName, keyword)
                .orderByDesc(OrderInfo::getCreatedAt);
        return orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Page<OrderInfo> getSellerOrders(Long sellerId, int pageNum, int pageSize, Integer status, String keyword) {
        try {
            List<Long> sellerOrderIds = new java.util.ArrayList<>();

            // 1. 查询该卖家所有商品的ID
            LambdaQueryWrapper<Goods> goodsWrapper = new LambdaQueryWrapper<>();
            goodsWrapper.eq(Goods::getSellerId, sellerId)
                       .select(Goods::getId);
            List<Goods> sellerGoods = goodsMapper.selectList(goodsWrapper);

            if (sellerGoods == null || sellerGoods.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }

            List<Long> goodsIds = new java.util.ArrayList<>();
            for (Goods g : sellerGoods) {
                goodsIds.add(g.getId());
            }

            // 2. 查询包含这些商品的订单项，获取订单ID列表
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(OrderItem::getGoodsId, goodsIds)
                      .select(OrderItem::getOrderId);
            List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);

            if (orderItems == null || orderItems.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }

            for (OrderItem item : orderItems) {
                if (!sellerOrderIds.contains(item.getOrderId())) {
                    sellerOrderIds.add(item.getOrderId());
                }
            }

            // 3. 查询这些订单（支持状态和关键词筛选）
            LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.in(OrderInfo::getId, sellerOrderIds)
                       .eq(status != null, OrderInfo::getStatus, status)
                       .and(StrUtil.isNotBlank(keyword), w -> w
                           .like(OrderInfo::getOrderNo, keyword)
                           .or()
                           .like(OrderInfo::getReceiverName, keyword))
                       .orderByDesc(OrderInfo::getCreatedAt);

            return orderMapper.selectPage(new Page<>(pageNum, pageSize), orderWrapper);

        } catch (Exception e) {
            System.err.println("查询卖家订单失败: " + e.getMessage());
            e.printStackTrace();
            return new Page<>(pageNum, pageSize);
        }
    }

    @Override
    public OrderInfo getOrderDetail(Long orderId, Long userId) {
        OrderInfo order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        if (userId != null && !order.getUserId().equals(userId)) {
            return null;
        }
        return order;
    }

    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String datePart = LocalDateTime.now().format(formatter);
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + datePart + uuidPart;
    }
}