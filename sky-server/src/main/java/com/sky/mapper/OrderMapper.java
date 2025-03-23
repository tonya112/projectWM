package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    void insertOrder(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);


    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} where id = #{id}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, Long id);


    Page<Orders> pageQueryHistory(OrdersPageQueryDTO orderPageQueryDTO);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    @Select("select count(id) from orders where status = #{status}")
    Integer countByStatus(Integer toBeConfirmed);

    @Select("select * from orders where status = #{pendingPayment} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTime(Integer pendingPayment, LocalDateTime orderTime);


    void autoCancelTimeoutOrder(Integer cancelled, Integer accessWord, String cancelReason, LocalDateTime cancelTime, List<Long> ids);

    @Select("select * from orders where status = #{status}")
    List<Orders> getByStatus(Integer deliveryInProgress);

    void autoCompleteDeliveryOrder(Integer completed, Integer accessWord, LocalDateTime deliveryTime, List<Long> ids);

    Integer countByMap(Map map);

    Double sumByMap(Map map);
}
