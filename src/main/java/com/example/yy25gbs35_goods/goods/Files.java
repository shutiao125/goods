package com.example.yy25gbs35_goods.goods;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("files")
public class Files implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String fileName;
    
    private String originalName;
    
    private String filePath;
    
    private Long fileSize;
    
    private String fileType;
    
    private String extension;
    
    private String description;
    
    private String uploadUser;
    
    private LocalDateTime uploadTime;
    
    private Integer downloadCount;
    
    private Integer isDeleted;
}
