package com.example.goods.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goods.goods.Tag;
import com.example.goods.mapper.TagMapper;
import com.example.goods.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Override
    public List<Tag> getAllTags() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getCreatedAt);
        return tagMapper.selectList(wrapper);
    }

    @Override
    public Page<Tag> getTagsByPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), Tag::getName, keyword)
                .or()
                .like(StrUtil.isNotBlank(keyword), Tag::getSlug, keyword)
                .orderByDesc(Tag::getCreatedAt);
        return tagMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Tag getTagById(Integer id) {
        return tagMapper.selectById(id);
    }

    @Override
    public boolean addTag(Tag tag) {
        return tagMapper.insert(tag) > 0;
    }

    @Override
    public boolean updateTag(Tag tag) {
        return tagMapper.updateById(tag) > 0;
    }

    @Override
    public boolean deleteTag(Integer id) {
        return tagMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteTags(List<Integer> ids) {
        return tagMapper.deleteBatchIds(ids) > 0;
    }
}
