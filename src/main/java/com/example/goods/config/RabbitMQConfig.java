package com.example.goods.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String ARTICLE_EXCHANGE = "article.exchange";

    // 队列名称
    public static final String ARTICLE_NOTIFICATION_QUEUE = "article.notification.queue";

    // 路由键
    public static final String ARTICLE_NOTIFICATION_ROUTING_KEY = "article.notification";

    /**
     * 创建主题交换机
     */
    @Bean
    public TopicExchange articleExchange() {
        return new TopicExchange(ARTICLE_EXCHANGE);
    }

    /**
     * 创建文章通知队列
     */
    @Bean
    public Queue articleNotificationQueue() {
        return QueueBuilder.durable(ARTICLE_NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding articleNotificationBinding(Queue articleNotificationQueue, TopicExchange articleExchange) {
        return BindingBuilder.bind(articleNotificationQueue)
                .to(articleExchange)
                .with(ARTICLE_NOTIFICATION_ROUTING_KEY);
    }

    /**
     * 配置消息转换器为JSON格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("消息发送失败: " + cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("消息未被路由: " + returned.getMessage());
        });

        return rabbitTemplate;
    }
}
