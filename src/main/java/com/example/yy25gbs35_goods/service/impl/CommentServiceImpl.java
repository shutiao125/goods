package com.example.yy25gbs35_goods.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yy25gbs35_goods.goods.Comment;
import com.example.yy25gbs35_goods.mapper.CommentMapper;
import com.example.yy25gbs35_goods.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public List<Comment> getAllComments() {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Comment::getCreatedAt);
        return commentMapper.selectList(wrapper);
    }

    @Override
    public Page<Comment> getCommentsByPage(int pageNum, int pageSize, String status, Long goodsId, String targetType) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Comment::getStatus, Integer.parseInt(status));
        }
        if (goodsId != null) {
            wrapper.eq(Comment::getGoodsId, goodsId);
        }
        if (StrUtil.isNotBlank(targetType)) {
            wrapper.eq(Comment::getTargetType, Byte.parseByte(targetType));
        }
        wrapper.orderByDesc(Comment::getCreatedAt);
        return commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Comment getCommentById(Long id) {
        return commentMapper.selectById(id);
    }

    @Override
    public boolean addComment(Comment comment) {
        return commentMapper.insert(comment) > 0;
    }

    @Override
    public boolean updateComment(Comment comment) {
        return commentMapper.updateById(comment) > 0;
    }

    @Override
    public boolean deleteComment(Long id) {
        return commentMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteComments(List<Long> ids) {
        return commentMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public boolean approveComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            comment.setStatus(1); // 已通过
            return commentMapper.updateById(comment) > 0;
        }
        return false;
    }

    @Override
    public boolean rejectComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            comment.setStatus(2); // 已拒绝
            return commentMapper.updateById(comment) > 0;
        }
        return false;
    }

    @Override
    public List<Comment> getCommentsByGoodsId(Long goodsId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getGoodsId, goodsId)
                .eq(Comment::getStatus, 1) // 只显示已通过的评论
                .orderByDesc(Comment::getCreatedAt);
        return commentMapper.selectList(wrapper);
    }
}