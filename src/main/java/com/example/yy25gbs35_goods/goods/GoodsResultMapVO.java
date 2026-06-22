package com.example.yy25gbs35_goods.goods;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象（VO），用于MyBatis ResultMap关联查询
 * 包含作者信息和分类信息（一对一关联）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsResultMapVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String description;
    private String mainImage;
    private String imageList;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer salesCount;
    private Long categoryId;
    private Integer status;
    private Long sellerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime scheduledTime;
    
    // 扩展实体属性 - 关联对象
    private User seller;      // 卖家信息（一对一关联）
    private Category category; // 分类信息（一对一关联）
}

