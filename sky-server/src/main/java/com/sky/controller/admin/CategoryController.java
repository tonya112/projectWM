package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.sky.constant.StatusConstant.DISABLE;

@Slf4j
@RequestMapping("/admin/category")
@RestController
@Api(tags = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @ApiOperation("新增分类")
    @PostMapping
    public Result saveCategory(@RequestBody CategoryDTO categoryDTO) {
        log.info("新增分类 {}", categoryDTO);

        categoryService.save(categoryDTO);
        return Result.success();
    }

    @ApiOperation("分类分页查询")
    @GetMapping("page")
    public Result<PageResult> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        log.info("分类分页查询 {}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        log.info("分类分页查询结果 {}", pageResult);
        return Result.success(pageResult);
    }

    @ApiOperation("修改分类")
    @PutMapping
    public Result update(@RequestBody CategoryDTO categoryDTO) {
        log.info("修改分类 {}", categoryDTO);

        categoryService.update(categoryDTO);

        return Result.success();
    }

    @ApiOperation("启用/禁用分类")
    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status, @RequestParam("id") Long id){
        log.info("启用/禁用分类：{},{}",status,id);
        categoryService.startOrStop(status, id);
        return Result.success();
    }

    @ApiOperation("根据id删除分类")
    @DeleteMapping
    public Result deleteById(@RequestParam("id") Long id) {
        log.info("根据id删除分类：{}", id);
        categoryService.deleteById(id);
        return Result.success();
    }

    @ApiOperation("根据类型查询分类")
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type) {
        log.info("根据类型查询分类：{}", type);
        List<Category> categoryList = new ArrayList<>();
        categoryList = categoryService.getByType(type);
        log.info("根据类型查询分类结果：{}", categoryList);
        return Result.success(categoryList);

    }

}
