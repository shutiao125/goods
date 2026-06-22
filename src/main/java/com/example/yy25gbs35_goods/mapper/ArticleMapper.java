package com.example.yy25gbs35_goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.yy25gbs35_goods.goods.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    
    @Select("SELECT * FROM articles WHERE status = 1 AND published_at IS NOT NULL ORDER BY is_top DESC, published_at DESC")
    List<Article> findPublishedArticles();
    
    @Select("SELECT * FROM articles WHERE status = 1 AND published_at IS NOT NULL AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%') OR topic LIKE CONCAT('%', #{keyword}, '%')) ORDER BY is_top DESC, published_at DESC")
    List<Article> searchArticles(@Param("keyword") String keyword);
}