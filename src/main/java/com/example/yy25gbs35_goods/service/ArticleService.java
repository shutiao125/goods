package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.Article;
import java.util.List;

public interface ArticleService extends IService<Article> {
    
    List<Article> getPublishedArticles();
    
    List<Article> searchArticles(String keyword);
    
    Article getArticleById(Integer id);
    
    void incrementViewCount(Integer id);
    
    void executeScheduledPublish();
    
    List<Article> getScheduledArticles();
}