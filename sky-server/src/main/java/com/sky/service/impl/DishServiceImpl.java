package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService{
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

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
}
