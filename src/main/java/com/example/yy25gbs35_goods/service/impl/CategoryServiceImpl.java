package com.example.yy25gbs35_goods.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yy25gbs35_goods.goods.Category;
import com.example.yy25gbs35_goods.mapper.CategoryMapper;
import com.example.yy25gbs35_goods.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> getAllCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getOrderNum);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public Page<Category> getCategoriesByPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), Category::getName, keyword)
                .or()
                .like(StrUtil.isNotBlank(keyword), Category::getDescription, keyword)
                .orderByAsc(Category::getOrderNum);
        return categoryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Category getCategoryById(Integer id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public boolean addCategory(Category category) {
        return categoryMapper.insert(category) > 0;
    }

    @Override
    public boolean updateCategory(Category category) {
        return categoryMapper.updateById(category) > 0;
    }

    @Override
    public boolean deleteCategory(Integer id) {
        return categoryMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteCategories(List<Integer> ids) {
        return categoryMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public List<Category> getCategoriesByParentId(Integer parentId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(parentId != null, Category::getParentId, parentId)
                .orderByAsc(Category::getOrderNum);
        return categoryMapper.selectList(wrapper);
    }
}
