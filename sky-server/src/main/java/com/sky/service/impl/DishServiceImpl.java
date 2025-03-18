package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishMapSetmealDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sky.constant.StatusConstant.DISABLE;
import static com.sky.constant.StatusConstant.ENABLE;

@Slf4j
@Service
public class DishServiceImpl implements DishService{
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setMealMapper;

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish newDish = new Dish();
        BeanUtils.copyProperties(dishDTO, newDish);

        dishMapper.insert(newDish);

        Long dishId = newDish.getId();
        log.info("新增菜品，菜品id：{}", dishId);

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors == null || flavors.isEmpty()){
            return;
        }

        flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));

        dishFlavorMapper.insertBatch(flavors);


    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> pages = dishMapper.pageQuery(dishPageQueryDTO);
        PageResult pageResult = new PageResult();
        pageResult.setTotal(pages.getTotal());
        pageResult.setRecords(pages.getResult());
        return pageResult;
    }

    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //检查是否起售
        List<Dish> dishes = dishMapper.getByIds(ids);
        if(dishes == null || dishes.isEmpty()){
            return;
        }
        List<Dish> dishENabledList = new ArrayList<>();
        dishes.forEach(dish -> {
            if(Objects.equals(dish.getStatus(), ENABLE)){
                dishENabledList.add(dish);
            }
        });

        if(!dishENabledList.isEmpty()){
            throw new DeletionNotAllowedException(dishENabledList + MessageConstant.DISH_ON_SALE);
        }

        //检查是否关联套餐
        List<DishMapSetmealDTO> dishSetmealMaps = setMealMapper.getSetmealIdsByDishIds(ids);
        List<DishMapSetmealDTO> relatedSetmealList = new ArrayList<>();
        dishSetmealMaps.forEach(dishMapSetmealDTO -> {
            if(dishMapSetmealDTO.getSetmealIds() != null){
                relatedSetmealList.add(dishMapSetmealDTO);
            }
        });

        if(!relatedSetmealList.isEmpty()){
            throw new DeletionNotAllowedException(relatedSetmealList + MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品以及口味数据
        deleteDishAndFlavor(ids);


    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        DishVO dishVO = dishMapper.getByIdWithFlavor(id);
        return dishVO;
    }

    @Transactional
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish newDish = new Dish();
        BeanUtils.copyProperties(dishDTO, newDish);
        List<Long> dishFlavorId = new ArrayList<>();
        dishFlavorId.add(dishDTO.getId());
        //先删除
        dishFlavorMapper.deleteByDishId(dishFlavorId);
        //再添加
        dishMapper.update(newDish);
        if(dishDTO.getFlavors() == null || dishDTO.getFlavors().isEmpty()){
            return;
        }
        dishDTO.getFlavors().forEach(dishFlavor -> dishFlavor.setDishId(dishDTO.getId()));
        dishFlavorMapper.insertBatch(dishDTO.getFlavors());

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Dish newDish = Dish.builder()
                .status(status)
                .id(id)
                .build();
        dishMapper.update(newDish);
    }

    @Transactional
    public void deleteDishAndFlavor(List<Long> ids){
        dishMapper.deleteBatch(ids);
        dishFlavorMapper.deleteByDishId(ids);
    }
}
