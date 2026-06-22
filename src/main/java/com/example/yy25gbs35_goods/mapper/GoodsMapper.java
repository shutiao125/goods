package com.example.yy25gbs35_goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yy25gbs35_goods.goods.Goods;
import com.example.yy25gbs35_goods.goods.GoodsResultMapVO;
import com.example.yy25gbs35_goods.goods.GoodsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.FetchType;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.util.List;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
    
    @Select("SELECT \n" +
            "    g.*, \n" +
            "    u.username AS seller_username, \n" +
            "    u.avatar AS seller_avatar,\n" +
            "    u.email AS seller_email,\n" +
            "    c.name AS category_name,\n" +
            "    c.slug AS category_slug\n" +
            "FROM products g\n" +
            "LEFT JOIN user u ON g.seller_id = u.id\n" +
            "LEFT JOIN categories c ON g.category_id = c.id\n" +
            "${ew.customSqlSegment}")
    List<GoodsVO> getPublishedGoodsVOsForHome(@Param(Constants.WRAPPER) Wrapper wrapper);
    
    @Select("SELECT * FROM products ${ew.customSqlSegment}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "mainImage", column = "main_image"),
        @Result(property = "imageList", column = "image_list"),
        @Result(property = "price", column = "price"),
        @Result(property = "originalPrice", column = "original_price"),
        @Result(property = "stock", column = "stock"),
        @Result(property = "salesCount", column = "sales_count"),
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "status", column = "status"),
        @Result(property = "sellerId", column = "seller_id"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "scheduledTime", column = "scheduled_time"),
        @Result(property = "seller", column = "seller_id", one = @One(select = "com.example.yy25gbs35_goods.mapper.UserMapper.selectById", fetchType = FetchType.LAZY)),
        @Result(property = "category", column = "category_id", one = @One(select = "com.example.yy25gbs35_goods.mapper.CategoryMapper.selectById", fetchType = FetchType.LAZY))
    })
    Page<GoodsResultMapVO> getGoodsResultMapVOsByPage(Page<GoodsResultMapVO> page, @Param(Constants.WRAPPER) Wrapper wrapper);
}
