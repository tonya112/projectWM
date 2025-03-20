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
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    @Autowired
    private RedisTemplate redisTemplate;

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

        //清除缓存
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);

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

        //清理缓存
        String key = "dish_*";
        cleanCache(key);


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

        //清理缓存
        log.info("修改菜品impl：{}", dishDTO);
        String key = "dish_*";
        cleanCache(key);
        //

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

        //清理缓存
        String key = "dish_*";
        cleanCache(key);
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        List<Dish> dishList = dishMapper.getByCategoryId(categoryId);
        return dishList;
    }

    @Transactional
    public void deleteDishAndFlavor(List<Long> ids){
        dishMapper.deleteBatch(ids);
        dishFlavorMapper.deleteByDishId(ids);

        //清理缓存
        String key = "dish_*";
        cleanCache(key);
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {

        Long categoryId = dish.getCategoryId();
        //构造redis中的key
        String key = "dish_" + categoryId;
        //查询redis中是否存在菜品数据

        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (list != null && !list.isEmpty()) {
            //如果存在，直接返回
            return list;
        }

        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        redisTemplate.opsForValue().set(key, dishVOList);

        return dishVOList;
    }

    private void cleanCache(String pattern){
        log.info("清理缓存：{}", pattern);
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
