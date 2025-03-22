package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult pageQueryHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderVO getOrderDetailById(Long id);

    void cancelOrder(Long id);

    void repetition(Long id);

    PageResult adminPageQueryHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void orderComfirmation(OrdersConfirmDTO ordersConfirmDTO);

    void orderRejection(OrdersRejectionDTO ordersRejectionDTO);

    void orderCancellation(OrdersCancelDTO ordersCancelDTO);

    void orderDelivery(Long id);

    void orderComplete(Long id);
}
