package com.example.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.goods.goods.Cart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
