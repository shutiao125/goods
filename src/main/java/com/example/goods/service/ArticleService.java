package com.example.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goods.goods.Article;
import java.util.List;

public interface ArticleService extends IService<Article> {
    
    List<Article> getPublishedArticles();
    
    List<Article> searchArticles(String keyword);
    
    Article getArticleById(Integer id);
    
    void incrementViewCount(Integer id);
    
    void executeScheduledPublish();
    
    List<Article> getScheduledArticles();
}