package com.example.goods.controller;

import com.example.goods.common.Result;
import com.example.goods.goods.Article;
import com.example.goods.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleApiController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public Result<List<Article>> getAllArticles() {
        List<Article> articles = articleService.getPublishedArticles();
        return Result.success(articles);
    }

    @GetMapping("/search")
    public Result<List<Article>> searchArticles(@RequestParam(required = false) String keyword) {
        List<Article> articles = articleService.searchArticles(keyword);
        return Result.success(articles);
    }

    @GetMapping("/{id}")
    public Result<Article> getArticleById(@PathVariable Integer id) {
        Article article = articleService.getArticleById(id);
        if (article == null) {
            return Result.error("文章不存在");
        }
        return Result.success(article);
    }

    @PostMapping
    public Result<String> createArticle(@RequestBody Article article) {
        boolean success = articleService.save(article);
        if (success) {
            return Result.success("文章创建成功", "创建成功");
        }
        return Result.error("文章创建失败");
    }

    @PutMapping("/{id}")
    public Result<String> updateArticle(@PathVariable Integer id, @RequestBody Article article) {
        article.setId(id);
        boolean success = articleService.updateById(article);
        if (success) {
            return Result.success("文章更新成功", "更新成功");
        }
        return Result.error("文章更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteArticle(@PathVariable Integer id) {
        boolean success = articleService.removeById(id);
        if (success) {
            return Result.success("文章删除成功", "删除成功");
        }
        return Result.error("文章删除失败");
    }
}