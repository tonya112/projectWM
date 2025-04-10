package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingcartService {
    void add(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> list();

    void sub(ShoppingCartDTO shoppingCartDTO);

    void clean();
}
