package com.example.goods.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncFileService {
    @Async("fileUploadExecutor")
    public void asyncSaveCoverImage(byte[] fileBytes, String uploadPath) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 确保使用系统原生路径分隔符
            String systemPath = uploadPath.replace("/", java.io.File.separator);
            
            // 创建父目录
            cn.hutool.core.io.FileUtil.mkdir(cn.hutool.core.io.FileUtil.getParent(systemPath, 1));
            
            // 保存文件
            java.nio.file.Path filePath = java.nio.file.Paths.get(systemPath);
            java.nio.file.Files.write(filePath, fileBytes);
            
            long fileSaveTime = System.currentTimeMillis();
            
            if (isImageFile(uploadPath)) {
                // 提取文件名（不含扩展名）和扩展名
                String fileName = new java.io.File(systemPath).getName();
                String extension = "";
                int dotIndex = fileName.lastIndexOf(".");
                if (dotIndex > 0) {
                    extension = fileName.substring(dotIndex);
                    fileName = fileName.substring(0, dotIndex);
                }
                
                // 生成缩略图路径：保存到 zoom_img 子目录
                String baseDir = new java.io.File(systemPath).getParent();
                String thumbnailDir = baseDir + java.io.File.separator + "zoom_img";
                
                // 确保 zoom_img 目录存在
                cn.hutool.core.io.FileUtil.mkdir(thumbnailDir);
                
                // 缩略图文件名：原文件名_thumb.jpg
                String thumbnailFileName = fileName + "_thumb" + extension;
                String thumbnailPath = thumbnailDir + java.io.File.separator + thumbnailFileName;
                
                generateThumbnailInternal(filePath.toString(), thumbnailPath);
                
                long endTime = System.currentTimeMillis();
                
                System.out.println("=== 文件处理（异步线程）耗时统计 ===");
                System.out.println("线程名称: " + Thread.currentThread().getName());
                System.out.println("开始时间戳: " + startTime);
                System.out.println("保存物理文件: " + (fileSaveTime - startTime) + " ms");
                System.out.println("生成缩略图: " + (endTime - fileSaveTime) + " ms");
                System.out.println("异步总耗时: " + (endTime - startTime) + " ms");
                System.out.println("文件路径: " + systemPath);
                System.out.println("缩略图路径: " + thumbnailPath);
                System.out.println("======================================");
            } else {
                long endTime = System.currentTimeMillis();
                
                System.out.println("=== 文件处理（异步线程）耗时统计 ===");
                System.out.println("线程名称: " + Thread.currentThread().getName());
                System.out.println("开始时间戳: " + startTime);
                System.out.println("保存物理文件: " + (fileSaveTime - startTime) + " ms");
                System.out.println("异步总耗时: " + (endTime - startTime) + " ms");
                System.out.println("文件路径: " + systemPath);
                System.out.println("======================================");
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            System.err.println("异步保存封面图失败：" + e.getMessage());
            System.err.println("异步耗时: " + (endTime - startTime) + " ms");
            e.printStackTrace();
        }
    }

    private void generateThumbnailInternal(String filePath, String thumbnailPath) {
        try {
            cn.hutool.core.img.ImgUtil.scale(
                    new java.io.File(filePath),
                    new java.io.File(thumbnailPath),
                    0.2f
            );
        } catch (Exception e) {
            System.err.println("生成封面缩略图失败：" + e.getMessage());
        }
    }

    private boolean isImageFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".png")
                || lowerName.endsWith(".gif") || lowerName.endsWith(".jpeg");
    }
}

