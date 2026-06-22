package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yy25gbs35_goods.goods.Goods;
import com.example.yy25gbs35_goods.goods.GoodsResultMapVO;
import com.example.yy25gbs35_goods.goods.GoodsVO;

import java.time.LocalDateTime;
import java.util.List;

public interface GoodsService {
    
    long count();
    
    List<Goods> getAllGoods();
    
    Goods getGoodsById(Long id);
    
    Goods getGoodsByIdWithCache(Long id);
    
    void saveGoods(Goods goods);
    
    List<Goods> searchGoods(String keyword, String startDate, String endDate);
    
    Page<Goods> searchGoodsWithPage(String keyword, String startDate, String endDate, Integer pageNum, Integer pageSize);
    
    void updateGoods(Goods goods);
    
    void deleteGoods(Long id);
    
    void batchDeleteGoods(List<Long> ids);
    
    Goods getArticleById(Integer id);
    
    Goods updateArticle(Goods article);
    
    boolean deleteArticle(Integer id);
    
    boolean deleteArticles(List<Integer> ids);
    
    void schedulePublish(Integer goodsId, LocalDateTime publishTime);
    
    void executeScheduledPublish();
    
    List<Goods> getScheduledGoods();
    
    Page<Goods> getScheduledGoodsPage(Integer pageNum, Integer pageSize);
    
    void cancelScheduledPublish(Integer goodsId);
    
    void saveScheduledGoods(Goods goods, LocalDateTime publishTime);
    
    List<Goods> getPublishedGoods();
    
    Page<Goods> getPublishedGoodsPage(Integer pageNum, Integer pageSize);
    
    List<GoodsVO> getPublishedGoodsVOsForHome(String keyword);
    
    Page<GoodsResultMapVO> getGoodsResultMapVOsByPage(Integer pageNum, Integer pageSize, String keyword, String startDate, String endDate);
    
    Page<Goods> getGoodsByPage(Integer pageNum, Integer pageSize, String keyword, Integer categoryId, Integer status);
    
    Page<GoodsResultMapVO> getSellerGoodsByPage(Long sellerId, Integer pageNum, Integer pageSize, String keyword, String startDate, String endDate);

    int countBySellerId(Long sellerId);

    int countActiveGoodsBySellerId(Long sellerId);

    int getTotalSalesBySellerId(Long sellerId);
}