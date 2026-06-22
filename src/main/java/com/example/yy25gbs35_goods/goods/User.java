package com.example.yy25gbs35_goods.goods;


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
@TableName("user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String avatar;
    private String role;           // USER / ADMIN / SELLER
    private String status;         // ACTIVE / BANNED
    private String bio;
    private String website;
    
    // 卖家专属字段（普通用户可为null）
    private String companyName;      // 公司名称/店铺名称
    private String contactPhone;     // 联系电话
    private String businessLicense;  // 营业执照号
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}