package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    //每分钟触发一次
    //定时处理超时订单
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        //选取超时订单
        log.info("定时处理超时订单");
        //计算超时15分钟
        LocalDateTime orderTime =  LocalDateTime.now().plusMinutes(-15);
        List<Orders> timeoutOrder = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, orderTime);
        List<Long> ids = new ArrayList<>();
        if(timeoutOrder == null || timeoutOrder.isEmpty()){
            return;
        }
        ids = timeoutOrder.stream().map(Orders::getId).collect(Collectors.toList());

        log.info("超时订单：{}", ids);

        String cancelReason = "订单超时未支付，已取消";
        orderMapper.autoCancelTimeoutOrder(Orders.CANCELLED, Orders.PENDING_PAYMENT, cancelReason, LocalDateTime.now(), ids);
    }

    //自动完成为确认的派送中订单
    @Scheduled(cron = "0 0 1 * * ?")
    //@Scheduled(cron = "0/10 * * * * ?")
    public void processDeliveryOrder(){
        log.info("自动完成为确认的派送中订单");
        LocalDateTime orderTime = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orders = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, orderTime);
        List<Long> ids = new ArrayList<>();
        if(orders == null || orders.isEmpty()){
            return;
        }
        ids = orders.stream().map(Orders::getId).collect(Collectors.toList());

        log.info("派送中订单自动完成：{}", ids);
        orderMapper.autoCompleteDeliveryOrder(Orders.COMPLETED, Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now(), ids);
    }

}
