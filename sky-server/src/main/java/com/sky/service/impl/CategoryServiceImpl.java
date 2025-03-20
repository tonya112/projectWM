package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

import static com.sky.constant.MessageConstant.CATEGORY_BE_RELATED_BY_DISH;
import static com.sky.constant.MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL;
import static com.sky.constant.StatusConstant.DISABLE;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void save(CategoryDTO categoryDTO) {
        Category newCategory = new Category();
        BeanUtils.copyProperties(categoryDTO, newCategory);

        newCategory.setStatus(DISABLE);
        //newCategory.setCreateTime(LocalDateTime.now());
        //newCategory.setUpdateTime(LocalDateTime.now());
        //newCategory.setCreateUser(BaseContext.getCurrentId());
        //newCategory.setUpdateUser(BaseContext.getCurrentId());
        categoryMapper.insert(newCategory);
    }

    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        PageResult pageResult = new PageResult();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(page.getResult());
        return pageResult;
    }

    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        //category.setUpdateTime(LocalDateTime.now());
        //category.setUpdateUser(BaseContext.getCurrentId());
        categoryMapper.update(category);
    }



    @Override
    public void deleteById(Long id) {
        //判断当前分类是否关联了菜品
        Integer count = dishMapper.countByCategoryId(id);

        //关联了就抛出异常
        if(count > 0){
            throw new DeletionNotAllowedException(CATEGORY_BE_RELATED_BY_DISH);
        }

        //判断当前分类是否关联了套餐

        count = setmealMapper.countByCategoryId(id);

        if(count > 0){
            throw new DeletionNotAllowedException(CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //没有关联执行删除
        categoryMapper.deleteById(id);
    }

    @Override
    public List<Category> getByType(Integer type) {
        return categoryMapper.getByType(type);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .status(status)
                .id(id)
                .build();
        categoryMapper.update(category);
    }




}
