package com.example.yy25gbs35_goods.goods;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品视图对象（VO），用于前端展示
 * 包含商品基本信息以及关联的分类、卖家等扩展信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 商品基本信息
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

    // 扩展信息 - 分类相关
    private String categoryName;     // 分类名称
    private String categorySlug;     // 分类slug

    // 扩展信息 - 卖家相关
    private String sellerUsername;   // 卖家用户名
    private String sellerAvatar;     // 卖家头像
    private String sellerEmail;      // 卖家邮箱

    // 其他展示用字段
    private List<String> imageUrls;  // 图片URL列表（从imageList解析）
    private Boolean isPublished;     // 是否已发布
    private String displayStatus;    // 显示状态（中文描述）
}
