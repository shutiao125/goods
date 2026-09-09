package com.example.goods.goods;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("comments")
public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private Long goodsId;

    private String username;

    private Integer rating;

    private Long userId;

    private Byte targetType;

    private Long targetId;

    private Long parentId;

    private Integer status;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime createdAt;

    private String reply;  // 管理员回复内容

    private LocalDateTime replyTime;  // 回复时间

    private Long repliedBy;  // 回复管理员ID
}