package com.example.yy25gbs35_goods.goods;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("products")
public class Goods implements Serializable{
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

}
