package com.example.goods.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.common.Result;
import com.example.goods.goods.Comment;
import com.example.goods.goods.Goods;
import com.example.goods.service.CommentService;
import com.example.goods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods")
public class GoodsApiController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private CommentService commentService;

    @GetMapping
    public Result<List<Goods>> getAllGoods() {
        List<Goods> goods = goodsService.getAllGoods();
        return Result.success(goods);
    }

    @GetMapping("/published")
    public Result<List<Goods>> getPublishedGoods() {
        List<Goods> goods = goodsService.getPublishedGoods();
        return Result.success(goods);
    }

    @GetMapping("/{id}")
    public Result<Goods> getGoodsById(@PathVariable Long id) {
        Goods goods = goodsService.getGoodsById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        return Result.success(goods);
    }

    @GetMapping("/page")
    public Result<Page<Goods>> getGoodsByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId) {
        
        Page<Goods> page = goodsService.getGoodsByPage(pageNum, pageSize, keyword, categoryId, null);
        return Result.success(page);
    }

    @GetMapping("/seller/{sellerId}")
    public Result<List<Goods>> getGoodsBySellerId(@PathVariable Long sellerId) {
        List<Goods> goods = goodsService.getAllGoods(); // 简化实现
        return Result.success(goods);
    }

    @PostMapping
    public Result<String> createGoods(@RequestBody Goods goods) {
        try {
            goodsService.saveGoods(goods);
            return Result.success("商品创建成功", "创建成功");
        } catch (Exception e) {
            return Result.error("商品创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<String> updateGoods(@PathVariable Long id, @RequestBody Goods goods) {
        try {
            goods.setId(id);
            goodsService.updateGoods(goods);
            return Result.success("商品更新成功", "更新成功");
        } catch (Exception e) {
            return Result.error("商品更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteGoods(@PathVariable Long id) {
        try {
            goodsService.deleteGoods(id);
            return Result.success("商品删除成功", "删除成功");
        } catch (Exception e) {
            return Result.error("商品删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/comments")
    public Result<List<Comment>> getComments(@PathVariable Long id) {
        List<Comment> comments = commentService.getCommentsByGoodsId(id);
        return Result.success(comments);
    }
}