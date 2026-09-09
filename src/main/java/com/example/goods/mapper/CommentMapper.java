package com.example.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.goods.goods.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
