package com.example.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goods.goods.ArticleGoods;
import com.example.goods.mapper.ArticleGoodsMapper;
import com.example.goods.service.ArticleGoodsService;
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