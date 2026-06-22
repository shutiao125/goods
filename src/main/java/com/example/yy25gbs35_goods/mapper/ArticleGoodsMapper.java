package com.example.yy25gbs35_goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.yy25gbs35_goods.goods.ArticleGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleGoodsMapper extends BaseMapper<ArticleGoods> {
    
    @Select("SELECT * FROM article_goods WHERE article_id = #{articleId} ORDER BY sort ASC, id ASC")
    List<ArticleGoods> findByArticleId(@Param("articleId") Integer articleId);
}