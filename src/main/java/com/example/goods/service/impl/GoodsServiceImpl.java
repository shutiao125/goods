package com.example.goods.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.goods.config.NotificationEmailConfig;
import com.example.goods.goods.ArticleNotificationMessage;
import com.example.goods.goods.Goods;
import com.example.goods.goods.GoodsResultMapVO;
import com.example.goods.goods.GoodsVO;
import com.example.goods.mapper.GoodsMapper;
import com.example.goods.service.ArticleNotificationProducer;
import com.example.goods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@CacheConfig(cacheNames = "products")
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private ArticleNotificationProducer notificationProducer;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private NotificationEmailConfig notificationEmailConfig;

    @Value("${server.port:8081}")
    private String serverPort;

    @Override
    public long count() {
        return goodsMapper.selectCount(null);
    }

    @Override
    public List<Goods> getAllGoods() {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Goods::getStatus, 1)
                .or(inner -> inner.eq(Goods::getStatus, 0)
                        .isNotNull(Goods::getScheduledTime)
                        .le(Goods::getScheduledTime, LocalDateTime.now())));
        wrapper.orderByDesc(Goods::getCreatedAt);
        return goodsMapper.selectList(wrapper);
    }

    @Override
    public Goods getGoodsById(Long id) {
        // 直接根据ID查询，不限制状态，确保编辑时能获取到所有状态的商品
        return goodsMapper.selectById(id);
    }

    @Override
    @Cacheable(key = "#id")
    public Goods getGoodsByIdWithCache(Long id) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getId, id)
                .and(w -> w.eq(Goods::getStatus, 1)
                        .or(inner -> inner.eq(Goods::getStatus, 0)
                                .isNotNull(Goods::getScheduledTime)
                                .le(Goods::getScheduledTime, LocalDateTime.now())));
        return goodsMapper.selectOne(wrapper);
    }

    @Override
    public void saveGoods(Goods goods) {
        goods.setCreatedAt(LocalDateTime.now());
        goods.setUpdatedAt(LocalDateTime.now());
        if (goods.getStatus() == null) {
            goods.setStatus(1);
        }
        if (goods.getStock() == null) {
            goods.setStock(0);
        }
        if (goods.getSalesCount() == null) {
            goods.setSalesCount(0);
        }
        if (goods.getSellerId() == null) {
            goods.setSellerId(1L);
        }
        
        if (goods.getScheduledTime() != null && goods.getScheduledTime().isAfter(LocalDateTime.now())) {
            goods.setStatus(0);
            goodsMapper.insert(goods);
        } else {
            goods.setStatus(1);
            goodsMapper.insert(goods);
            sendNotification(goods, "publish");
        }
    }

    @Override
    public List<Goods> searchGoods(String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Goods::getName, keyword)
                    .or()
                    .like(Goods::getDescription, keyword));
        }

        if (StringUtils.hasText(startDate)) {
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(Goods::getCreatedAt, startDateTime);
        }

        if (StringUtils.hasText(endDate)) {
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            wrapper.le(Goods::getCreatedAt, endDateTime);
        }

        wrapper.and(w -> w.eq(Goods::getStatus, 1)
                .or(inner -> inner.eq(Goods::getStatus, 0)
                        .isNotNull(Goods::getScheduledTime)
                        .le(Goods::getScheduledTime, LocalDateTime.now())));
        wrapper.orderByDesc(Goods::getCreatedAt);

        return goodsMapper.selectList(wrapper);
    }

    @Override
    public Page<Goods> searchGoodsWithPage(String keyword, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Goods::getName, keyword)
                    .or()
                    .like(Goods::getDescription, keyword));
        }

        if (StringUtils.hasText(startDate)) {
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(Goods::getCreatedAt, startDateTime);
        }

        if (StringUtils.hasText(endDate)) {
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            wrapper.le(Goods::getCreatedAt, endDateTime);
        }

        wrapper.and(w -> w.eq(Goods::getStatus, 1)
                .or(inner -> inner.eq(Goods::getStatus, 0)
                        .isNotNull(Goods::getScheduledTime)
                        .le(Goods::getScheduledTime, LocalDateTime.now())));
        wrapper.orderByDesc(Goods::getCreatedAt);

        Page<Goods> page = new Page<>(pageNum, pageSize);

        return goodsMapper.selectPage(page, wrapper);
    }

    @Override
    public void updateGoods(Goods goods) {
        goods.setUpdatedAt(LocalDateTime.now());
        
        // 只有当用户勾选了"设置定时上线时间"复选框时，才自动调整状态
        // 否则保持用户手动选择的状态不变
        if (goods.getScheduledTime() != null) {
            if (goods.getScheduledTime().isAfter(LocalDateTime.now())) {
                // 定时时间在未来，设置为下架(0)
                goods.setStatus(0);
            } else {
                // 定时时间已到或已过，设置为上架(1)
                goods.setStatus(1);
            }
        }
        // 如果没有设置scheduledTime，保持用户选择的状态不变
        
        goodsMapper.updateById(goods);
        sendNotification(goods, "update");
    }

    @Override
    public void deleteGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods != null) {
            goodsMapper.deleteById(id);
            sendNotification(goods, "delete");
        }
    }

    @Override
    public void batchDeleteGoods(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            for (Long id : ids) {
                Goods goods = goodsMapper.selectById(id);
                if (goods != null) {
                    sendNotification(goods, "delete");
                }
            }
            goodsMapper.deleteBatchIds(ids);
        }
    }

    @Override
    @Cacheable(key = "#id")
    public Goods getArticleById(Integer id) {
        return goodsMapper.selectById(id);
    }

    @Override
    @CachePut(key = "#article.id")
    public Goods updateArticle(Goods article) {
        goodsMapper.updateById(article);
        return article;
    }

    @Override
    @CacheEvict(key = "#id")
    public boolean deleteArticle(Integer id) {
        Goods goods = goodsMapper.selectById(id);
        boolean result = goodsMapper.deleteById(id) > 0;
        if (result && goods != null) {
            sendNotification(goods, "delete");
        }
        return result;
    }

    @Override
    public boolean deleteArticles(List<Integer> ids) {
        for (Integer id : ids){
            Goods goods = goodsMapper.selectById(id);
            if (goods != null) {
                sendNotification(goods, "delete");
            }
            cacheManager.getCache("articles").evict(id);
        }
        return goodsMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public void schedulePublish(Integer goodsId, LocalDateTime publishTime) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods != null) {
            goods.setStatus(0);
            goods.setScheduledTime(publishTime);
            goodsMapper.updateById(goods);
        }
    }

    @Override
    public void executeScheduledPublish() {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getStatus, 0)
                .isNotNull(Goods::getScheduledTime)
                .le(Goods::getScheduledTime, LocalDateTime.now())
                .orderByAsc(Goods::getScheduledTime);

        List<Goods> scheduledGoods = goodsMapper.selectList(wrapper);

        if (scheduledGoods.isEmpty()) {
            return;
        }

        System.out.println("发现 " + scheduledGoods.size() + " 个待发布商品，开始发布...");

        for (Goods goods : scheduledGoods) {
            try {
                goods.setStatus(1);
                goodsMapper.updateById(goods);
                sendNotification(goods, "publish");
                System.out.println("商品发布成功: ID=" + goods.getId() + ", 名称=" + goods.getName());
            } catch (Exception e) {
                System.err.println("商品发布失败: ID=" + goods.getId() + ", 错误=" + e.getMessage());
            }
        }
    }

    @Override
    public List<Goods> getScheduledGoods() {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getStatus, 0)
                .isNotNull(Goods::getScheduledTime)
                .orderByAsc(Goods::getScheduledTime);
        return goodsMapper.selectList(wrapper);
    }

    @Override
    public Page<Goods> getScheduledGoodsPage(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getStatus, 0)
                .isNotNull(Goods::getScheduledTime)
                .gt(Goods::getScheduledTime, LocalDateTime.now())
                .orderByAsc(Goods::getScheduledTime);

        Page<Goods> page = new Page<>(pageNum, pageSize);

        return goodsMapper.selectPage(page, wrapper);
    }

    @Override
    public void cancelScheduledPublish(Integer goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods != null && goods.getStatus() == 0) {
            goods.setStatus(2);
            goodsMapper.updateById(goods);
        }
    }

    @Override
    public void saveScheduledGoods(Goods goods, LocalDateTime publishTime) {
        goods.setStatus(0);
        goods.setScheduledTime(publishTime);
        goods.setUpdatedAt(LocalDateTime.now());
        if (goods.getStock() == null) {
            goods.setStock(0);
        }
        if (goods.getSalesCount() == null) {
            goods.setSalesCount(0);
        }
        if (goods.getSellerId() == null) {
            goods.setSellerId(1L);
        }
        goodsMapper.insert(goods);
    }

    @Override
    public List<Goods> getPublishedGoods() {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getStatus, 1)
               .orderByDesc(Goods::getCreatedAt);
        return goodsMapper.selectList(wrapper);
    }

    @Override
    public Page<Goods> getPublishedGoodsPage(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getStatus, 1)
               .orderByDesc(Goods::getCreatedAt);
        
        Page<Goods> page = new Page<>(pageNum, pageSize);
        
        return goodsMapper.selectPage(page, wrapper);
    }

    @Override
    public List<GoodsVO> getPublishedGoodsVOsForHome(String keyword) {
        QueryWrapper<GoodsVO> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("g.status", 1);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like("g.name", keyword)
                            .or()
                            .like("g.description", keyword)
            );
        }

        queryWrapper.orderByDesc("g.created_at");
        
        return goodsMapper.getPublishedGoodsVOsForHome(queryWrapper);
    }

    @Override
    public Page<GoodsResultMapVO> getGoodsResultMapVOsByPage(Integer pageNum, Integer pageSize, String keyword, String startDate, String endDate) {
        Page<GoodsResultMapVO> pageParam = new Page<>(pageNum, pageSize);
        
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();

        // 显示所有状态的商品（包括上架、下架、缺货），让管理员可以看到完整的商品列表
        // 如果需要筛选特定状态，可以通过前端传参实现
        
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like("name", keyword)
                            .or()
                            .like("description", keyword)
            );
        }
        
        if (StrUtil.isNotBlank(startDate)) {
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            queryWrapper.ge("created_at", startDateTime);
        }
        
        if (StrUtil.isNotBlank(endDate)) {
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            queryWrapper.le("created_at", endDateTime);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        System.out.println("📦 [商品列表查询] 页码=" + pageNum + ", 每页=" + pageSize + 
                           ", 关键词=" + (keyword != null ? keyword : "无") +
                           ", 开始日期=" + (startDate != null ? startDate : "无") +
                           ", 结束日期=" + (endDate != null ? endDate : "无"));
        
        Page<GoodsResultMapVO> result = goodsMapper.getGoodsResultMapVOsByPage(pageParam, queryWrapper);
        
        System.out.println("✅ [商品列表结果] 总数=" + result.getTotal() + 
                           ", 当前页记录数=" + (result.getRecords() != null ? result.getRecords().size() : 0) +
                           ", 总页数=" + result.getPages());
        
        return result;
    }

    private void sendNotification(Goods goods, String type) {
        try {
            if (!notificationEmailConfig.isEnabled()) {
                System.out.println("📧 邮件通知已禁用，跳过发送");
                return;
            }

            if (notificationEmailConfig.getRecipients() == null || notificationEmailConfig.getRecipients().isEmpty()) {
                System.out.println("⚠️ 未配置邮件接收人，跳过发送");
                return;
            }

            String goodsUrl = "http://localhost:" + serverPort + "/goods/" + goods.getId();

            ArticleNotificationMessage message = new ArticleNotificationMessage();
            message.setArticleId(goods.getId().intValue());
            message.setTitle(goods.getName());
            message.setContentSummary(goods.getDescription());
            message.setPrice(goods.getPrice() != null ? goods.getPrice().toString() : "0.00");
            message.setNotificationType(type);
            message.setGoodsUrl(goodsUrl);
            message.setRecipientEmails(notificationEmailConfig.getRecipients());
            message.setPublishTime(LocalDateTime.now());

            notificationProducer.sendArticleNotification(message);
            System.out.println("✅ 邮件通知消息已发送到队列: type=" + type + ", goods=" + goods.getName());

        } catch (Exception e) {
            System.err.println("❌ 发送邮件通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Page<Goods> getGoodsByPage(Integer pageNum, Integer pageSize, String keyword, Integer categoryId, Integer status) {
        Page<Goods> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Goods::getName, keyword)
                    .or()
                    .like(Goods::getDescription, keyword));
        }
        
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Goods::getCategoryId, categoryId);
        }
        
        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }
        
        wrapper.orderByDesc(Goods::getCreatedAt);
        
        return goodsMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<GoodsResultMapVO> getSellerGoodsByPage(Long sellerId, Integer pageNum, Integer pageSize, String keyword, String startDate, String endDate) {
        Page<GoodsResultMapVO> pageParam = new Page<>(pageNum, pageSize);
        
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        
        // 只显示该卖家发布的商品
        queryWrapper.eq("seller_id", sellerId);
        
        // 显示所有状态的商品（上架、下架、缺货）
        // 不限制 status，让卖家可以看到自己发布的所有商品
        
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like("name", keyword)
                            .or()
                            .like("description", keyword)
            );
        }
        
        if (StrUtil.isNotBlank(startDate)) {
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            queryWrapper.ge("created_at", startDateTime);
        }
        
        if (StrUtil.isNotBlank(endDate)) {
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            queryWrapper.le("created_at", endDateTime);
        }
        
        queryWrapper.orderByDesc("created_at");

        return goodsMapper.getGoodsResultMapVOsByPage(pageParam, queryWrapper);
    }

    @Override
    public int countBySellerId(Long sellerId) {
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId);
        return goodsMapper.selectCount(queryWrapper).intValue();
    }

    @Override
    public int countActiveGoodsBySellerId(Long sellerId) {
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId)
                   .eq("status", 1); // 1=上架状态
        return goodsMapper.selectCount(queryWrapper).intValue();
    }

    @Override
    public int getTotalSalesBySellerId(Long sellerId) {
        QueryWrapper<Goods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("seller_id", sellerId)
                   .isNotNull("sales_count")
                   .gt("sales_count", 0);

        List<Goods> goodsList = goodsMapper.selectList(queryWrapper);
        int totalSales = 0;
        for (Goods goods : goodsList) {
            if (goods.getSalesCount() != null) {
                totalSales += goods.getSalesCount();
            }
        }
        return totalSales;
    }
}