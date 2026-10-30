package com.example.goods.utils;

public class EmailTemplateUtils {
    
    /**
     * 构建商品发布通知邮件HTML
     * @param goodsName 商品名称
     * @param goodsPrice 商品价格
     * @param goodsUrl 商品链接
     * @return HTML内容
     */
    public static String buildGoodsPublishEmail(String goodsName, String goodsPrice, String goodsUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }" +
                ".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }" +
                ".content { padding: 30px; }" +
                ".goods-name { font-size: 20px; font-weight: bold; color: #333; margin-bottom: 10px; }" +
                ".goods-price { font-size: 24px; color: #dc3545; font-weight: bold; margin: 15px 0; }" +
                ".btn { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 25px; margin-top: 20px; }" +
                ".footer { background: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'><h1>🎉 新商品发布通知</h1></div>" +
                "<div class='content'>" +
                "<p>您好！</p>" +
                "<p>有一个新商品已经发布，详细信息如下：</p>" +
                "<div class='goods-name'>" + escapeHtml(goodsName) + "</div>" +
                "<div class='goods-price'>¥" + escapeHtml(goodsPrice) + "</div>" +
                "<a href='" + escapeHtml(goodsUrl) + "' class='btn'>查看详情</a>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>此邮件由系统自动发送，请勿回复</p>" +
                "<p>&copy; 2024 内容电商平台</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * 构建验证码邮件HTML
     * @param username 用户名
     * @param code 验证码
     * @param expireMinutes 有效期（分钟）
     * @return HTML内容
     */
    public static String buildVerificationCodeEmail(String username, String code, int expireMinutes) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }" +
                ".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
                ".code { font-size: 32px; font-weight: bold; color: #667eea; text-align: center; padding: 20px; background: #f8f9ff; border-radius: 8px; margin: 20px 0; letter-spacing: 5px; }" +
                ".warning { color: #dc3545; font-size: 14px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<h2>🔐 验证码</h2>" +
                "<p>您好，" + escapeHtml(username) + "！</p>" +
                "<p>您的验证码是：</p>" +
                "<div class='code'>" + escapeHtml(code) + "</div>" +
                "<p class='warning'>⚠️ 验证码将在 " + expireMinutes + " 分钟后过期，请勿泄露给他人。</p>" +
                "<p>如果这不是您本人的操作，请忽略此邮件。</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * HTML转义，防止XSS攻击
     */
    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
