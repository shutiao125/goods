package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.ArticleGoods;
import java.util.List;

public interface ArticleGoodsService extends IService<ArticleGoods> {
    
    List<ArticleGoods> getGoodsByArticleId(Integer articleId);
}