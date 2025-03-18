package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DishMapSetmealDTO implements Serializable {
    private Long dishId;
    private List<Long> setmealIds;
}
