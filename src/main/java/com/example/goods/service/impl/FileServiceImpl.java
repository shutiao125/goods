package com.example.goods.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goods.exception.BusinessException;
import com.example.goods.goods.Files;
import com.example.goods.mapper.FileMapper;
import com.example.goods.service.AsyncFileService;
import com.example.goods.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, Files> implements FileService {

    @Value("${file.upload.path:D:/blog_files}")
    private String uploadPath;

    @Autowired
    private AsyncFileService asyncFileService;

    public FileServiceImpl() {
        if (!FileUtil.exist(uploadPath)) {
            FileUtil.mkdir(uploadPath);
        }
    }

    @Override
    public Files uploadFile(MultipartFile file, String uploadUser, String description) {
        long startTime = System.currentTimeMillis();
        
        try {
            String originalName = file.getOriginalFilename();
            String extension = FileUtil.extName(originalName);
            
            // 使用原始文件名（避免UUID导致文件名不一致）
            String fileName = originalName;
            
            // 如果文件名已存在，添加时间戳避免覆盖
            Path targetPath = Paths.get(uploadPath);
            FileUtil.mkdir(targetPath.toString());
            
            java.io.File targetFile = targetPath.resolve(fileName).toFile();
            if (targetFile.exists()) {
                // 文件名冲突，添加时间戳
                String nameWithoutExt = originalName.substring(0, originalName.lastIndexOf("."));
                fileName = nameWithoutExt + "_" + System.currentTimeMillis() + "." + extension;
            }

            byte[] fileBytes = file.getBytes();
            long readTime = System.currentTimeMillis();

            Files fileEntity = new Files();
            fileEntity.setFileName(fileName);
            fileEntity.setOriginalName(originalName);
            // 直接存储文件名到数据库（不带路径）
            fileEntity.setFilePath(fileName);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileType(file.getContentType());
            fileEntity.setExtension(extension);
            fileEntity.setDescription(description);
            fileEntity.setUploadUser(uploadUser);
            fileEntity.setUploadTime(java.time.LocalDateTime.now());
            fileEntity.setDownloadCount(0);
            fileEntity.setIsDeleted(0);

            this.save(fileEntity);
            long dbSaveTime = System.currentTimeMillis();

            // 传递完整的物理文件路径给异步方法
            String fullPath = targetPath.resolve(fileName).toString();
            long asyncTriggerTime = System.currentTimeMillis();
            asyncFileService.asyncSaveCoverImage(fileBytes, fullPath);
            long endTime = System.currentTimeMillis();

            System.out.println("=== 文件上传（主线程）耗时统计 ===");
            System.out.println("开始时间戳: " + startTime);
            System.out.println("读取文件内容: " + (readTime - startTime) + " ms");
            System.out.println("保存数据库记录: " + (dbSaveTime - readTime) + " ms");
            System.out.println("触发异步任务: " + (asyncTriggerTime - dbSaveTime) + " ms");
            System.out.println("主线程总耗时: " + (endTime - startTime) + " ms");
            System.out.println("原始文件名: " + originalName);
            System.out.println("保存文件名: " + fileName);
            System.out.println("文件大小: " + file.getSize() + " bytes");
            System.out.println("注意：异步任务在后台继续执行，与主线程有时间差");
            System.out.println("=====================================");

            return fileEntity;

        } catch (Exception e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    private boolean isImageFile(String extension) {
        if (extension == null) return false;
        String lowerExt = extension.toLowerCase();
        return lowerExt.equals("jpg") || lowerExt.equals("png")
                || lowerExt.equals("gif") || lowerExt.equals("jpeg") || lowerExt.equals("bmp");
    }

    public Page<Files> getFileList(Integer pageNum, Integer pageSize, String fileName, String fileType) {
        LambdaQueryWrapper<Files> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(fileName)) {
            wrapper.like(Files::getFileName, fileName);
        }
        if (StrUtil.isNotBlank(fileType)) {
            wrapper.eq(Files::getFileType, fileType);
        }
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<Files> getFileListByUser(String uploadUser) {
        LambdaQueryWrapper<Files> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Files::getUploadUser, uploadUser);
        return this.list(wrapper);
    }

    @Override
    public boolean deleteFilePhysical(Integer id) {
        Files file = this.getById(id);
        if (file != null) {
            try {
                Path targetPath = Paths.get(uploadPath, file.getFilePath());
                java.nio.file.Files.deleteIfExists(Paths.get(targetPath.toString()));
            } catch (IOException e) {
                throw new RuntimeException("删除物理文件失败：" + e.getMessage(), e);
            }
            return this.removeById(id);
        }
        return false;
    }

    @Override
    public void incrementDownloadCount(Integer fileId) {
        if (fileId == null || fileId <= 0) {
            throw new BusinessException(400, "文件ID无效");
        }
        
        Files file = this.getById(fileId);
        if (file == null) {
            throw new BusinessException(404, "文件不存在");
        }
        
        Integer currentCount = file.getDownloadCount();
        if (currentCount == null) {
            currentCount = 0;
        }
        
        file.setDownloadCount(currentCount + 1);
        this.updateById(file);
    }
}
