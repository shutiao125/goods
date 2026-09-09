package com.example.goods.service.impl;

import com.example.goods.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            System.out.println("简单邮件发送成功: " + to);
        } catch (Exception e) {
            System.err.println("简单邮件发送失败: " + e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("HTML邮件发送成功: " + to);
        } catch (MessagingException e) {
            System.err.println("HTML邮件发送失败: " + e.getMessage());
            throw new RuntimeException("HTML邮件发送失败", e);
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String text, File attachmentFile) {
        try {
            if (attachmentFile == null || !attachmentFile.exists()) {
                throw new IllegalArgumentException("附件文件不存在");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            
            FileSystemResource file = new FileSystemResource(attachmentFile);
            helper.addAttachment(file.getFilename(), file);
            
            mailSender.send(message);
            System.out.println("带附件邮件发送成功: " + to);
        } catch (MessagingException e) {
            System.err.println("带附件邮件发送失败: " + e.getMessage());
            throw new RuntimeException("带附件邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlContent, File attachmentFile) {
        try {
            if (attachmentFile == null || !attachmentFile.exists()) {
                throw new IllegalArgumentException("附件文件不存在");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            FileSystemResource file = new FileSystemResource(attachmentFile);
            helper.addAttachment(file.getFilename(), file);
            
            mailSender.send(message);
            System.out.println("带附件HTML邮件发送成功: " + to);
        } catch (MessagingException e) {
            System.err.println("带附件HTML邮件发送失败: " + e.getMessage());
            throw new RuntimeException("带附件HTML邮件发送失败", e);
        }
    }
}
