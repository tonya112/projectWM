package com.sky.mapper;

import com.sky.dto.DishMapSetmealDTO;
import com.sky.entity.SetmealDish;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealDishMapper {

    List<DishMapSetmealDTO> getSetmealIdsByDishIds(List<Long> ids);

    void insertBatch(List<SetmealDish> setmealDishes);

    SetmealVO getByIdWithDishes(Long id);

    void deleteBySetmealId(Long id);

    void deleteBySetmealIds(List<Long> ids);

    List<SetmealDish> getDishBySetmealId(Long id);
}
