package com.example.goods.goods;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("articles")
public class Article implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Long userId;
    
    private String title;
    
    private String content;
    
    private String coverImage;
    
    private String imageList;
    
    private String videoUrl;
    
    private String topic;
    
    private String location;
    
    private Integer viewCount;
    
    private LocalDateTime publishedAt;
    
    private Integer status;
    
    private Integer isTop;
    
    /**
     * 关联的商品ID列表（JSON数组格式，如：[1,2,3]）
     * 用于手动指定推荐商品，优先级高于智能匹配
     */
    private String relatedGoodsIds;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 是否为当前登录用户（非数据库字段，仅用于前端权限控制）
     */
    @TableField(exist = false)
    private boolean isOwner;

    public boolean isOwner() {
        return this.isOwner;
    }

    public void setIsOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }
}