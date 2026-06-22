package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> getAllCategories();

    Page<Category> getCategoriesByPage(int pageNum, int pageSize, String keyword);

    Category getCategoryById(Integer id);

    boolean addCategory(Category category);

    boolean updateCategory(Category category);

    boolean deleteCategory(Integer id);

    boolean deleteCategories(List<Integer> ids);

    List<Category> getCategoriesByParentId(Integer parentId);
}
