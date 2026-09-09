package com.example.goods.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.access-path}")
    private String accessPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = new File(uploadPath).getAbsolutePath();
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }
        
        System.out.println("=== 文件上传配置 ===");
        System.out.println("配置的上传路径: " + uploadPath);
        System.out.println("绝对路径: " + absolutePath);
        System.out.println("访问路径: " + accessPath);
        System.out.println("完整映射: " + accessPath + " -> file:" + absolutePath);
        
        // 修改配置以正确映射 /uploads/goods/ 到指定目录
        registry.addResourceHandler("/uploads/goods/**")
                .addResourceLocations("file:" + absolutePath);
        
        System.out.println("资源处理器注册完成");
    }

    public String getUploadPath() {
        return uploadPath;
    }
}
