package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingcartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.sky.constant.MessageConstant.ADDRESS_BOOK_IS_NULL;
import static com.sky.constant.MessageConstant.SHOPPING_CART_IS_NULL;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingcartMapper shoppingcartMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;



    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //校验
        //1.地址
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        if (addressBookId == null) {
            throw new OrderBusinessException(ADDRESS_BOOK_IS_NULL);
        }
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if(addressBook == null){
            throw new OrderBusinessException(ADDRESS_BOOK_IS_NULL);
        }

        //2.购物车
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingcartMapper.getShoppingCartlistByUserId(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.isEmpty()){
            throw new OrderBusinessException(SHOPPING_CART_IS_NULL);
        }

        //校验合格
        List<String> addressParts = new ArrayList<>();
        addressParts.add(addressBook.getProvinceName());
        addressParts.add(addressBook.getCityName());
        addressParts.add(addressBook.getDistrictName());
        addressParts.add(addressBook.getDetail());

        String address = String.join(",", addressParts);

        //编辑并插入订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);

        order.setNumber(String.valueOf(System.currentTimeMillis()));//

        order.setStatus(Orders.PENDING_PAYMENT);
        order.setUserId(userId);
        order.setAddressBookId(addressBookId);
        order.setOrderTime(LocalDateTime.now());
        //order.setPayMethod(ordersSubmitDTO.getPayMethod());//
        order.setPayStatus(Orders.UN_PAID);
        //order.setAmount(ordersSubmitDTO.getAmount());//
        //order.setRemark(ordersSubmitDTO.getRemark());//
        order.setPhone(addressBook.getPhone());
        order.setAddress(address);
        order.setConsignee(addressBook.getConsignee());
        //order.setEstimatedDeliveryTime(ordersSubmitDTO.getEstimatedDeliveryTime());//
        //order.setDeliveryStatus(ordersSubmitDTO.getDeliveryStatus());//
        //order.setPackAmount(ordersSubmitDTO.getPackAmount());//
        //order.setTablewareNumber(ordersSubmitDTO.getTablewareNumber());//
        //order.setTablewareStatus(ordersSubmitDTO.getTablewareStatus());//
        orderMapper.insertOrder(order);

        //编辑并插入订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        shoppingCartList.forEach(cartItem -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cartItem, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        });
        orderDetailMapper.insertOrderDetailBatch(orderDetailList);

        //清空购物车
        shoppingcartMapper.cleanAllByUserId(userId);

        //封装返回VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return orderSubmitVO;
    }
}
