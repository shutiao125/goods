package com.example.yy25gbs35_goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yy25gbs35_goods.goods.ArticleGoods;
import com.example.yy25gbs35_goods.mapper.ArticleGoodsMapper;
import com.example.yy25gbs35_goods.service.ArticleGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleGoodsServiceImpl extends ServiceImpl<ArticleGoodsMapper, ArticleGoods> implements ArticleGoodsService {
    
    @Autowired
    private ArticleGoodsMapper articleGoodsMapper;
    
    @Override
    public List<ArticleGoods> getGoodsByArticleId(Integer articleId) {
        return articleGoodsMapper.findByArticleId(articleId);
    }
}