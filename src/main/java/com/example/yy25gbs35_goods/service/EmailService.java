package com.example.yy25gbs35_goods.service;

import java.io.File;

public interface EmailService {
    
    /**
     * 发送简单文本邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param text 邮件内容
     */
    void sendSimpleEmail(String to, String subject, String text);
    
    /**
     * 发送HTML邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML内容
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);
    
    /**
     * 发送带附件的邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param text 邮件内容
     * @param attachmentFile 附件文件
     */
    void sendEmailWithAttachment(String to, String subject, String text, File attachmentFile);
    
    /**
     * 发送带附件的HTML邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML内容
     * @param attachmentFile 附件文件
     */
    void sendHtmlEmailWithAttachment(String to, String subject, String htmlContent, File attachmentFile);
}
