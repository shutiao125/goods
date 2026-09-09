package com.example.goods.config;

import com.example.goods.goods.Article;
import com.example.goods.goods.Goods;
import com.example.goods.service.ArticleService;
import com.example.goods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduledPublishTask {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private ArticleService articleService;

    @Scheduled(cron = "0 * * * * ?")
    public void executeScheduledPublish() {
        try {
            System.out.println("⏰ 开始执行定时发布任务...");
            
            // 执行商品的定时发布
            System.out.println("📦 处理商品定时发布...");
            goodsService.executeScheduledPublish();
            
            // 执行文章的定时发布
            System.out.println("📝 处理文章定时发布...");
            articleService.executeScheduledPublish();
            
            System.out.println("✅ 定时发布任务执行完成");
        } catch (Exception e) {
            System.err.println("❌ 定时发布任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void logScheduledCount() {
        try {
            List<Goods> scheduledGoods = goodsService.getScheduledGoods();
            List<Article> scheduledArticles = articleService.getScheduledArticles();
            
            System.out.println("📊 [定时任务统计] 当前待发布商品数量: " + scheduledGoods.size());
            System.out.println("📊 [定时任务统计] 当前待发布文章数量: " + scheduledArticles.size());
            
            if (!scheduledGoods.isEmpty()) {
                System.out.println("📦 待发布商品列表:");
                for (Goods goods : scheduledGoods) {
                    System.out.println("   - ID=" + goods.getId() + 
                                     ", 名称=" + goods.getName() + 
                                     ", 计划时间=" + goods.getScheduledTime());
                }
            }
            
            if (!scheduledArticles.isEmpty()) {
                System.out.println("📝 待发布文章列表:");
                for (Article article : scheduledArticles) {
                    System.out.println("   - ID=" + article.getId() + 
                                     ", 标题=" + article.getTitle() + 
                                     ", 计划时间=" + article.getPublishedAt());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 统计待发布数量失败: " + e.getMessage());
        }
    }
}