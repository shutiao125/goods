package com.example.goods.service;

import com.example.goods.goods.ArticleNotificationMessage;
import com.example.goods.utils.EmailTemplateUtils;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ArticleNotificationConsumer {

    @Autowired
    private EmailService emailService;

    /**
     * 监听商品通知队列
     */
    @RabbitListener(queues = "article.notification.queue")
    public void handleArticleNotification(ArticleNotificationMessage message, Message amqpMessage, Channel channel) {
        try {
            System.out.println("收到商品通知消息: " + message);

            switch (message.getNotificationType()) {
                case "publish":
                    handleGoodsPublished(message);
                    break;
                case "update":
                    handleGoodsUpdated(message);
                    break;
                case "delete":
                    handleGoodsDeleted(message);
                    break;
                default:
                    System.out.println("未知通知类型: " + message.getNotificationType());
            }

            // 手动确认消息
            channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            System.err.println("处理商品通知消息失败: " + e.getMessage());
            try {
                // 拒绝消息并重新入队
                channel.basicNack(amqpMessage.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ioException) {
                System.err.println("拒绝消息失败: " + ioException.getMessage());
            }
        }
    }

    private void handleGoodsPublished(ArticleNotificationMessage message) {
        System.out.println("处理商品发布通知 - 商品ID: " + message.getArticleId()
                + ", 标题: " + message.getTitle()
                + ", 作者: " + message.getAuthorUsername());
        
        // 发送邮件通知
        sendGoodsPublishEmail(message);
    }

    private void handleGoodsUpdated(ArticleNotificationMessage message) {
        System.out.println("处理商品更新通知 - 商品ID: " + message.getArticleId()
                + ", 标题: " + message.getTitle());
        
        // 发送更新通知邮件（可选）
        // sendGoodsUpdateEmail(message);
    }

    private void handleGoodsDeleted(ArticleNotificationMessage message) {
        System.out.println("处理商品删除通知 - 商品ID: " + message.getArticleId()
                + ", 标题: " + message.getTitle());
        
        // 发送删除通知邮件（可选）
        // sendGoodsDeleteEmail(message);
    }

    /**
     * 发送商品发布邮件通知
     */
    private void sendGoodsPublishEmail(ArticleNotificationMessage message) {
        if (message.getRecipientEmails() == null || message.getRecipientEmails().isEmpty()) {
            System.out.println("收件人邮箱列表为空，跳过发送邮件");
            return;
        }

        try {
            String htmlContent = EmailTemplateUtils.buildGoodsPublishEmail(
                    message.getTitle(),
                    message.getPrice() != null ? message.getPrice() : "0.00",
                    message.getGoodsUrl() != null ? message.getGoodsUrl() : "#"
            );

            String subject = "🎉 新商品上架通知: " + message.getTitle();

            for (String recipientEmail : message.getRecipientEmails()) {
                try {
                    emailService.sendHtmlEmail(recipientEmail, subject, htmlContent);
                    System.out.println("成功发送邮件到: " + recipientEmail);
                } catch (Exception e) {
                    System.err.println("发送邮件到 " + recipientEmail + " 失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("处理商品发布邮件通知失败: " + e.getMessage());
        }
    }

    /**
     * 发送商品更新邮件通知（可选）
     */
    private void sendGoodsUpdateEmail(ArticleNotificationMessage message) {
        if (message.getRecipientEmails() == null || message.getRecipientEmails().isEmpty()) {
            return;
        }

        try {
            String subject = "📝 商品信息更新: " + message.getTitle();
            String text = "商品 " + message.getTitle() + " 已更新，请及时查看。";

            for (String recipientEmail : message.getRecipientEmails()) {
                emailService.sendSimpleEmail(recipientEmail, subject, text);
            }
        } catch (Exception e) {
            System.err.println("发送商品更新邮件失败: " + e.getMessage());
        }
    }

    /**
     * 发送商品删除邮件通知（可选）
     */
    private void sendGoodsDeleteEmail(ArticleNotificationMessage message) {
        if (message.getRecipientEmails() == null || message.getRecipientEmails().isEmpty()) {
            return;
        }

        try {
            String subject = "🗑️ 商品下架通知: " + message.getTitle();
            String text = "商品 " + message.getTitle() + " 已被下架。";

            for (String recipientEmail : message.getRecipientEmails()) {
                emailService.sendSimpleEmail(recipientEmail, subject, text);
            }
        } catch (Exception e) {
            System.err.println("发送商品删除邮件失败: " + e.getMessage());
        }
    }
}
