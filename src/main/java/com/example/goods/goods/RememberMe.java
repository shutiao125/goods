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
@TableName("remember_me")
public class RememberMe implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    private String passwordHash;

    private String series;  // Spring Security 系列标识符（主键）

    private String token;   // Spring Security 令牌值

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime lastUsed;  // 最后使用时间

    private Integer status;
}