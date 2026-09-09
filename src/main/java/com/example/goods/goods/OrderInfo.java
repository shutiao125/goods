package com.example.goods.goods;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("orders")
public class OrderInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private Integer status;  // 0待付款 1待发货 2已发货 3已完成 4已取消 5退款中

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private Integer payType;  // 1模拟支付

    private LocalDateTime payTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime completeTime;

    private String expressCompany;

    private String expressNo;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 非数据库字段：订单商品明细
    @TableField(exist = false)
    private List<OrderItem> items;
}