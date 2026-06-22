package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {
    List<Tag> getAllTags();

    Page<Tag> getTagsByPage(int pageNum, int pageSize, String keyword);

    Tag getTagById(Integer id);

    boolean addTag(Tag tag);

    boolean updateTag(Tag tag);

    boolean deleteTag(Integer id);

    boolean deleteTags(List<Integer> ids);
}
