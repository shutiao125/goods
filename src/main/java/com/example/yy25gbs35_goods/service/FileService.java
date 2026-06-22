package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.Files;
import org.springframework.web.multipart.MultipartFile;

public interface FileService extends IService<Files> {
    
    Files uploadFile(MultipartFile file, String uploadUser, String description);
    
    boolean deleteFilePhysical(Integer id);
    
    void incrementDownloadCount(Integer fileId);
}
