package com.sky.mapper;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingcartMapper {

    List<ShoppingCart> getByUserIdAndItemId(ShoppingCart shoppingCart);

    void insert(ShoppingCart shoppingCart);

    void update(ShoppingCart cartItem);

    List<ShoppingCart> getShoppingCartlistByUserId(ShoppingCart shoppingCart);

    void deleteByUserIdAndItemId(ShoppingCart shoppingCart);

    void cleanAllByUserId(Long userId);

    ShoppingCart fillShopingCartItemContent(ShoppingCart shoppingCart);
}
