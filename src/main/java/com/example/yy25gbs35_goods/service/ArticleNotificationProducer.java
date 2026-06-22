package com.example.yy25gbs35_goods.service;

import com.example.yy25gbs35_goods.goods.ArticleNotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleNotificationProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送商品发布通知
     */
    public void sendArticleNotification(ArticleNotificationMessage message) {
        rabbitTemplate.convertAndSend(
                "article.exchange",
                "article.notification",
                message
        );
    }
}
