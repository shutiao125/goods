package com.example.goods.config;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUploadUtil {

    private final String uploadPath;

    public FileUploadUtil(FileUploadConfig fileUploadConfig) {
        this.uploadPath = fileUploadConfig.getUploadPath();
        createUploadDirectory();
    }

    private void createUploadDirectory() {
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            System.out.println("创建上传目录: " + uploadDir.getAbsolutePath());
        } else {
            System.out.println("上传目录已存在: " + uploadDir.getAbsolutePath());
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String allowedExtensions = ".jpg.jpeg.png.gif.bmp.webp";
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new IOException("不支持的文件类型: " + extension);
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        
        // 创建完整目录路径
        Path dirPath = Paths.get(uploadPath);
        Files.createDirectories(dirPath);
        
        // 保存文件到 static/img 目录
        Path filePath = dirPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // 使用 /img/ 前缀存储到数据库（匹配静态资源映射）
        String accessUrl = "/img/" + fileName;
        
        System.out.println("=== 文件上传成功 ===");
        System.out.println("原始文件名: " + originalFilename);
        System.out.println("保存文件名: " + fileName);
        System.out.println("物理路径: " + filePath.toAbsolutePath());
        System.out.println("访问URL: " + accessUrl);
        System.out.println("文件大小: " + file.getSize() + " bytes");

        return accessUrl;
    }

    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            System.out.println("删除文件失败: 文件路径为空");
            return false;
        }

        // 提取文件名，处理正斜杠和反斜杠
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        // 如果还有反斜杠，再截取一次
        if (fileName.contains("\\")) {
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
        }
        
        File file = new File(uploadPath, fileName);
        
        System.out.println("尝试删除文件: " + file.getAbsolutePath());
        System.out.println("上传目录: " + uploadPath);
        System.out.println("文件名: " + fileName);
        
        boolean originalDeleted = false;
        boolean thumbnailDeleted = false;
        
        // 删除原图
        if (file.exists()) {
            originalDeleted = file.delete();
            System.out.println("删除原图结果: " + (originalDeleted ? "成功" : "失败"));
        } else {
            System.out.println("原图不存在，跳过删除");
        }
        
        // 生成缩略图文件名（原文件名_thumb.扩展名）
        String extension = "";
        String nameWithoutExt = fileName;
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex);
            nameWithoutExt = fileName.substring(0, dotIndex);
        }
        String thumbnailFileName = nameWithoutExt + "_thumb" + extension;
        
        // 构建缩略图路径（在 zoom_img 子目录中）
        File thumbnailFile = new File(uploadPath + File.separator + "zoom_img", thumbnailFileName);
        
        System.out.println("尝试删除缩略图: " + thumbnailFile.getAbsolutePath());
        System.out.println("缩略图文件名: " + thumbnailFileName);
        
        // 删除缩略图
        if (thumbnailFile.exists()) {
            thumbnailDeleted = thumbnailFile.delete();
            System.out.println("删除缩略图结果: " + (thumbnailDeleted ? "成功" : "失败"));
        } else {
            System.out.println("缩略图不存在，跳过删除");
        }
        
        // 只要有一个文件被删除就返回true
        boolean result = originalDeleted || thumbnailDeleted;
        System.out.println("整体删除结果: " + (result ? "成功" : "失败"));
        return result;
    }
}
