package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishMapSetmealDTO;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.sky.constant.MessageConstant.SETMEAL_ENABLE_FAILED;
import static com.sky.constant.StatusConstant.DISABLE;
import static com.sky.constant.StatusConstant.ENABLE;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishService;
    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        //添加setmeal
        Setmeal newSetmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, newSetmeal);
        setmealMapper.insert(newSetmeal);

        //添加setmealDish
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes == null || setmealDishes.isEmpty()){
            return;
        }
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(newSetmeal.getId()));
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        if(page == null){
            return null;
        }
        PageResult pageResult = new PageResult();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(page.getResult());
        return pageResult;
    }

    @Override
    public SetmealVO getByIdWithDishes(Long id) {
        SetmealVO setmealVO = setmealDishMapper.getByIdWithDishes(id);
        return setmealVO;
    }

    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        //更新setmeal
        Setmeal newSetmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, newSetmeal);
        setmealMapper.update(newSetmeal);

        //删除关联dish
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        //添加关联dish
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes == null || setmealDishes.isEmpty()){
            return;
        }
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(newSetmeal.getId()));
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //检查套餐是否起售
        if(status == ENABLE){
            //获取套餐关联菜品
            List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);

            if(setmealDishes != null && !setmealDishes.isEmpty()){
                List<Long> dishIds = setmealDishes.stream().map(SetmealDish::getDishId).collect(Collectors.toList());

                //检查套餐关联菜品是否都起售
                List<Dish> dishesInSetmeal = dishMapper.getByIds(dishIds);

                List<Dish> dishDisabledList = new ArrayList<>();
                dishesInSetmeal.forEach(dish -> {
                    if(Objects.equals(dish.getStatus(), DISABLE)){
                        dishDisabledList.add(dish);
                    }
                });

                if(!dishDisabledList.isEmpty()){
                    throw new SetmealEnableFailedException(SETMEAL_ENABLE_FAILED + dishDisabledList);
                }

            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //检查套餐是否起售
        List<Setmeal> setmeals = setmealMapper.getByIds(ids);

        List<Setmeal> setmealENabledList = new ArrayList<>();
        setmeals.forEach(setmeal -> {
            if(Objects.equals(setmeal.getStatus(), 1)){
                setmealENabledList.add(setmeal);
            }
        });
        //有起售的套餐无法删除，直接返回
        if(!setmealENabledList.isEmpty()){
            throw new DeletionNotAllowedException(setmealENabledList + MessageConstant.SETMEAL_ON_SALE);
        }
        //删除套餐
        setmealMapper.deleteBatch(ids);

        //删除关联套餐的SetmealDish
        setmealDishMapper.deleteBySetmealIds(ids);

    }


}
