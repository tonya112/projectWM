package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.ShoppingcartMapper;
import com.sky.service.ShoppingcartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShoppingcartServiceImpl implements ShoppingcartService {

    @Autowired
    private ShoppingcartMapper shoppingcartMapper;

    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        log.info("购物车物品DTO： {}", shoppingCartDTO);
        //赋值用户id转换成shoppingCart物品
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        log.info("购物车物品DTO to entity： {}", shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        log.info("购物车物品DTO set userID： {}", shoppingCart);


        //检测物品是否已经存在
        List<ShoppingCart> shoppingCartList = shoppingcartMapper.getByUserIdAndItemId(shoppingCart);

        //存在则修改数量
        if (shoppingCartList != null && !shoppingCartList.isEmpty()) {
            ShoppingCart cartItem = shoppingCartList.get(0);
            cartItem.setNumber(cartItem.getNumber() + 1);
            shoppingcartMapper.update(cartItem);
            return;
         }

        //不存在则添加到购物车
        //补充内容参数
        shoppingCart = shoppingcartMapper.fillShopingCartItemContent(shoppingCart); //mybatis映射会覆盖原始记录
        log.info("购物车物品： {}", shoppingCart);
        shoppingCart.setNumber(1);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        shoppingCart.setDishFlavor(shoppingCartDTO.getDishFlavor());
        shoppingCart.setDishId(shoppingCartDTO.getDishId());
        shoppingCart.setSetmealId(shoppingCartDTO.getSetmealId());
        shoppingCart.setCreateTime(LocalDateTime.now());
        //添加购物车
        shoppingcartMapper.insert(shoppingCart);
    }

    @Override
    public List<ShoppingCart> list() {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> list = shoppingcartMapper.getShoppingCartlistByUserId(shoppingCart);
        log.info("购物车物品： {}", list);
        return list;
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);

        //获取当前用户购物车对应物品信息
        List<ShoppingCart> shoppingCartList = shoppingcartMapper.getByUserIdAndItemId(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.isEmpty())
        {
            return;
        }
        if(shoppingCartList.get(0).getNumber() == 1)
        {
            shoppingcartMapper.deleteByUserIdAndItemId(shoppingCart);
            return;
        }
        shoppingCartList.get(0).setNumber(shoppingCartList.get(0).getNumber() - 1);
        shoppingcartMapper.update(shoppingCartList.get(0));

    }

    @Override
    public void clean() {
        Long userId = BaseContext.getCurrentId();
        shoppingcartMapper.cleanAllByUserId(userId);
    }
}
