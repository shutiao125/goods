package com.example.yy25gbs35_goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yy25gbs35_goods.config.NotificationEmailConfig;
import com.example.yy25gbs35_goods.goods.Article;
import com.example.yy25gbs35_goods.goods.ArticleNotificationMessage;
import com.example.yy25gbs35_goods.mapper.ArticleMapper;
import com.example.yy25gbs35_goods.service.ArticleService;
import com.example.yy25gbs35_goods.service.ArticleNotificationProducer;
import com.example.yy25gbs35_goods.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    
    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private ArticleNotificationProducer notificationProducer;
    
    @Autowired
    private NotificationEmailConfig notificationEmailConfig;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${server.port:8081}")
    private String serverPort;
    
    @Override
    public List<Article> getPublishedArticles() {
        return articleMapper.findPublishedArticles();
    }
    
    @Override
    public List<Article> searchArticles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPublishedArticles();
        }
        return articleMapper.searchArticles(keyword.trim());
    }
    
    @Override
    public Article getArticleById(Integer id) {
        try {
            Article article = articleMapper.selectById(id);
            if (article == null) {
                System.out.println("⚠️ [ArticleService] 文章不存在: ID=" + id);
                return null;
            }
            
            // 检查文章状态
            Integer status = article.getStatus();
            if (status == null || status <= 0) {
                System.out.println("⚠️ [ArticleService] 文章已删除或状态异常: ID=" + id + ", status=" + status);
                return null;
            }
            
            // 增加浏览量
            incrementViewCount(id);
            
            System.out.println("✅ [ArticleService] 文章加载成功: ID=" + id + ", 标题=" + article.getTitle());
            return article;
        } catch (Exception e) {
            System.err.println("❌ [ArticleService] 获取文章详情失败: ID=" + id + ", 错误=" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public void incrementViewCount(Integer id) {
        try {
            Article existingArticle = articleMapper.selectById(id);
            if (existingArticle != null) {
                Integer currentCount = existingArticle.getViewCount();
                if (currentCount == null) {
                    currentCount = 0;
                }

                Article updateArticle = new Article();
                updateArticle.setId(id);
                updateArticle.setViewCount(currentCount + 1);
                updateArticle.setUpdatedAt(java.time.LocalDateTime.now());
                articleMapper.updateById(updateArticle);
            }
        } catch (Exception e) {
            System.err.println("更新浏览量失败: " + e.getMessage());
        }
    }

    @Override
    public void executeScheduledPublish() {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 2)
               .isNotNull(Article::getPublishedAt)
               .le(Article::getPublishedAt, LocalDateTime.now())
               .orderByAsc(Article::getPublishedAt);

        List<Article> scheduledArticles = articleMapper.selectList(wrapper);

        if (scheduledArticles.isEmpty()) {
            return;
        }

        System.out.println("📝 发现 " + scheduledArticles.size() + " 篇待发布文章，开始发布...");

        for (Article article : scheduledArticles) {
            try {
                article.setStatus(1);
                article.setUpdatedAt(LocalDateTime.now());
                articleMapper.updateById(article);
                
                System.out.println("✅ 文章发布成功: ID=" + article.getId() + ", 标题=" + article.getTitle());
                
                // 发送邮件通知
                sendNotification(article, "publish");
            } catch (Exception e) {
                System.err.println("❌ 文章发布失败: ID=" + article.getId() + ", 错误=" + e.getMessage());
            }
        }
    }

    private void sendNotification(Article article, String type) {
        try {
            if (!notificationEmailConfig.isEnabled()) {
                System.out.println("📧 邮件通知已禁用，跳过发送");
                return;
            }

            if (notificationEmailConfig.getRecipients() == null || notificationEmailConfig.getRecipients().isEmpty()) {
                System.out.println("⚠️ 未配置邮件接收人，跳过发送");
                return;
            }

            String articleUrl = "http://localhost:" + serverPort + "/articles/" + article.getId();

            ArticleNotificationMessage message = new ArticleNotificationMessage();
            message.setArticleId(article.getId());
            message.setTitle(article.getTitle());
            message.setContentSummary(article.getContent() != null && article.getContent().length() > 100 
                ? article.getContent().substring(0, 100) + "..." 
                : article.getContent());
            message.setAuthorUsername("系统用户");
            message.setPublishTime(LocalDateTime.now());
            message.setNotificationType(type);
            message.setRecipientEmails(notificationEmailConfig.getRecipients());

            // 通过消息队列发送通知（如果 RabbitMQ 可用）
            try {
                notificationProducer.sendArticleNotification(message);
                System.out.println("✅ 文章通知消息已发送到队列: type=" + type + ", 文章=" + article.getTitle());
            } catch (Exception queueException) {
                // 如果消息队列不可用，直接发送邮件
                System.err.println("⚠️ 消息队列不可用，尝试直接发送邮件: " + queueException.getMessage());
                sendDirectEmail(article, type, articleUrl);
            }
        } catch (Exception e) {
            System.err.println("❌ 发送文章通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendDirectEmail(Article article, String type, String articleUrl) {
        try {
            String subject = "";
            String content = "";

            switch (type) {
                case "publish":
                    subject = "📝 新文章发布通知 - " + article.getTitle();
                    content = "<html><body style='font-family: Arial, sans-serif;'>" +
                             "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                             "<h2 style='color: #667eea;'>✨ 新文章已发布</h2>" +
                             "<div style='background: #f8f9fa; padding: 15px; border-radius: 8px; margin: 15px 0;'>" +
                             "<h3 style='margin: 0 0 10px 0;'>" + article.getTitle() + "</h3>" +
                             "<p style='color: #666; margin: 0;'>📌 话题: " + (article.getTopic() != null ? article.getTopic() : "未分类") + "</p>" +
                             "<p style='color: #666; margin: 5px 0 0 0;'>🕐 发布时间: " + 
                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>" +
                             "</div>" +
                             "<a href='" + articleUrl + "' style='display: inline-block; padding: 12px 24px; background: linear-gradient(135deg, #667eea, #764ba2); color: white; text-decoration: none; border-radius: 6px; margin: 15px 0;'>查看文章</a>" +
                             "<hr style='border: none; border-top: 1px solid #e9ecef; margin: 20px 0;'>" +
                             "<p style='color: #999; font-size: 12px; text-align: center;'>此邮件由系统自动发送，请勿回复</p>" +
                             "</div></body></html>";
                    break;

                case "update":
                    subject = "📝 文章更新通知 - " + article.getTitle();
                    content = "<html><body style='font-family: Arial, sans-serif;'>" +
                             "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                             "<h2 style='color: #ffc107;'>✏️ 文章已更新</h2>" +
                             "<div style='background: #fff3cd; padding: 15px; border-radius: 8px; margin: 15px 0;'>" +
                             "<h3 style='margin: 0 0 10px 0;'>" + article.getTitle() + "</h3>" +
                             "<p style='color: #666; margin: 0;'>更新时间: " + 
                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>" +
                             "</div>" +
                             "<a href='" + articleUrl + "' style='display: inline-block; padding: 12px 24px; background: #ffc107; color: white; text-decoration: none; border-radius: 6px; margin: 15px 0;'>查看文章</a>" +
                             "</div></body></html>";
                    break;

                default:
                    return;
            }

            for (String recipient : notificationEmailConfig.getRecipients()) {
                emailService.sendHtmlEmail(recipient, subject, content);
            }
            
            System.out.println("✅ 文章邮件通知直接发送成功: type=" + type + ", 收件人数量=" + notificationEmailConfig.getRecipients().size());
        } catch (Exception e) {
            System.err.println("❌ 直接发送文章邮件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Article> getScheduledArticles() {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 2)
               .isNotNull(Article::getPublishedAt)
               .orderByAsc(Article::getPublishedAt);
        return articleMapper.selectList(wrapper);
    }
}