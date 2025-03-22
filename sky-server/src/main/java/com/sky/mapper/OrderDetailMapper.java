package com.sky.mapper;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    void insertOrderDetailBatch(List<OrderDetail> orderDetailList);

    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(OrderDetail orderDetail);

    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getOrderDetailByOrderId(Long id);

    void update(Orders cancelOrder);
}
