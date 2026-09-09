package com.example.goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goods.goods.Comment;

import java.util.List;

public interface CommentService extends IService<Comment> {

    List<Comment> getAllComments();

    Page<Comment> getCommentsByPage(int pageNum, int pageSize, String status, Long goodsId, String targetType);

    Comment getCommentById(Long id);

    boolean addComment(Comment comment);

    boolean updateComment(Comment comment);

    boolean deleteComment(Long id);

    boolean deleteComments(List<Long> ids);

    boolean approveComment(Long id);

    boolean rejectComment(Long id);

    List<Comment> getCommentsByGoodsId(Long goodsId);
}