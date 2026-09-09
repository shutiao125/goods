package com.example.goods.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${file.upload.path}")
    private String uploadPath;

    @Autowired
    private UserContextInterceptor userContextInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 重要：明确指定只处理这些路径为静态资源
        // 避免其他路径（如/admin/orders/...）被误判为静态资源

        // 映射上传的文件目录（保留旧数据兼容性）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);

        // 映射项目内置的静态图片资源（新格式）- 使用绝对路径避免 classpath: 问题
        String imgPath = "src/main/resources/static/img/";
        if (!imgPath.startsWith("file:")) {
            imgPath = "file:" + imgPath;
        }
        registry.addResourceHandler("/img/**")
                .addResourceLocations(imgPath);

        // 映射根目录的图片文件（兼容数据库中直接存储文件名的情况）
        registry.addResourceHandler("/*.jpg", "/*.jpeg", "/*.png", "/*.gif", "/*.bmp", "/*.webp")
                .addResourceLocations(imgPath);

        // 映射缩略图目录
        String zoomImgPath = "src/main/resources/static/img/zoom_img/";
        if (!zoomImgPath.startsWith("file:")) {
            zoomImgPath = "file:" + zoomImgPath;
        }
        registry.addResourceHandler("/img/zoom_img/**")
                .addResourceLocations(zoomImgPath);

        // 保留原有的静态文件映射
        registry.addResourceHandler("/files/static/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/toLoginPage",
                        "/login",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/img/**",
                        "/static/**",
                        "/files/static/**"
                );
    }
}