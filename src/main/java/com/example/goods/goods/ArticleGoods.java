package com.example.goods.goods;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("article_goods")
public class ArticleGoods implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Integer articleId;
    
    private Long goodsId;
    
    private Integer sort; // 排序权重
    
    private LocalDateTime createdAt;
}