package com.example.yy25gbs35_goods.goods;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品发布通知消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleNotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Integer articleId;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品内容摘要
     */
    private String contentSummary;

    /**
     * 作者用户名
     */
    private String authorUsername;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 通知类型：publish-发布, update-更新, delete-删除
     */
    private String notificationType;

    /**
     * 收件人邮箱列表（用于发送邮件通知）
     */
    private List<String> recipientEmails;

    /**
     * 商品价格
     */
    private String price;

    /**
     * 商品链接
     */
    private String goodsUrl;
}
