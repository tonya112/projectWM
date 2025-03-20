package com.sky.service;

import com.github.pagehelper.Page;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface CategoryService {
    void save(CategoryDTO newCategoryDTO);

    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    void update(CategoryDTO categoryDTO);

    void deleteById(Long id);

    List<Category> getByType(Integer type);

    void startOrStop(Integer status, Long id);

}
