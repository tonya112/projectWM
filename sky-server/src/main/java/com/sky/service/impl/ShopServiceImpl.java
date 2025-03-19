package com.sky.service.impl;


import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {
    @Autowired
    private RedisTemplate redisTemplate;

    private static final String SHOP_STATUS_KEY = "SHOP_STATUS";

    @Override
    public void setStatus(Integer status) {
        redisTemplate.opsForValue().set(SHOP_STATUS_KEY, status.toString());

    }

    @Override
    public Integer getStatus() {
        Object value = redisTemplate.opsForValue().get(SHOP_STATUS_KEY);
        Integer status = value == null ? 0 : Integer.parseInt(value.toString());
        return status;
    }
}
