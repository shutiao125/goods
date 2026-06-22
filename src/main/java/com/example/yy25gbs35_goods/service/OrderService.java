package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.OrderInfo;

import java.util.List;

public interface OrderService extends IService<OrderInfo> {

    OrderInfo createOrder(Long userId, List<Long> cartIds, Long addressId, String remark);

    boolean payOrder(Long orderId, Long userId);

    boolean cancelOrder(Long orderId, Long userId);

    boolean adminCancelOrder(Long orderId);

    boolean shipOrder(Long orderId, String expressCompany, String expressNo);

    boolean completeOrder(Long orderId);

    Page<OrderInfo> getOrdersByUserId(Long userId, int pageNum, int pageSize, Integer status);

    Page<OrderInfo> getAllOrders(int pageNum, int pageSize, Integer status, String keyword);

    Page<OrderInfo> getSellerOrders(Long sellerId, int pageNum, int pageSize, Integer status, String keyword);

    OrderInfo getOrderDetail(Long orderId, Long userId);
}