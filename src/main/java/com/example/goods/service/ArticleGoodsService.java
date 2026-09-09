package com.example.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goods.goods.ArticleGoods;
import java.util.List;

public interface ArticleGoodsService extends IService<ArticleGoods> {
    
    List<ArticleGoods> getGoodsByArticleId(Integer articleId);
}