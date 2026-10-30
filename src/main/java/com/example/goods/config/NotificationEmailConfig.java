package com.example.goods.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "notification.email")
public class NotificationEmailConfig {
    
    private boolean enabled = true;
    
    private List<String> recipients;
    
    private String fromName = "内容电商平台";
}
