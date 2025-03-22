package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.task.WebSocketTask;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.sky.constant.MessageConstant.*;
import static com.sky.constant.StatusConstant.ENABLE;

@Slf4j
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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    private static Orders CUR_ORDER = null;



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

        CUR_ORDER = order; //全局变量抓取

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

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

/*        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, CUR_ORDER.getId());

        //通过websocket给客户端发送消息
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId",CUR_ORDER.getId());
        map.put("content","您有一笔订单待处理");
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);

        return vo;

    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        orders.setPayStatus(Orders.PAID);
        orders.setCheckoutTime(LocalDateTime.now());



/*
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
*/

        orderMapper.update(orders);
    }

    @Override
    public PageResult pageQueryHistory(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        Page<Orders> historyOrders = orderMapper.pageQueryHistory(ordersPageQueryDTO);

        //查询为空，则返回空
        if(historyOrders == null || historyOrders.getTotal() == 0){
            return new PageResult(0L, null);
        }

        //非空,准备封装VO
        List<OrderVO> orderVOList = new ArrayList<>();
        historyOrders.getResult().forEach(order -> {

            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(order.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderDetail);

            orderVO.setOrderDetailList(orderDetailList);
            orderVOList.add(orderVO);
        });

        PageResult pageResult = new PageResult(historyOrders.getTotal(), orderVOList);
        return pageResult;

    }

    @Override
    public OrderVO getOrderDetailById(Long id) {
        OrderVO orderVO = new OrderVO();
        //获取订单
        Orders order = orderMapper.getById(id);
        if(order == null){
            return orderVO;
        }

        //获取订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getOrderDetailByOrderId(order.getId());

        //封装
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;

    }


    @Override
    public void cancelOrder(Long id) {
        //获取订单
        Orders order = orderMapper.getById(id);
        if(order == null){
            throw new OrderBusinessException(ORDER_NOT_FOUND);
        }

        //判断状态
        Integer status = order.getStatus();
        if(!Objects.equals(status, Orders.TO_BE_CONFIRMED)){ //待接单
            log.error("订单不是待接单状态：{}", status);
            return;
        }

        //待接单状态下
        // TODO 退款

        //删除订单（逻辑删除）
        Orders cancelOrder = new Orders();
        cancelOrder.setId(id);
        cancelOrder.setCancelReason("用户取消");
        cancelOrder.setStatus(Orders.CANCELLED);
        orderDetailMapper.update(cancelOrder);

    }

    @Transactional
    @Override
    public void repetition(Long id) {

        //获取订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getOrderDetailByOrderId(id);
        Long userId = BaseContext.getCurrentId();

        //通过明细重新添加物品到购物车
        //归类
        List<Long> orderDishIdsList = new ArrayList<>();
        List<Long> orderSetmealIdsList = new ArrayList<>();
        orderDetailList.forEach(orderDetail -> {
            //分类
            if(orderDetail.getDishId() != null){
                Long dishId = orderDetail.getDishId();
                orderDishIdsList.add(dishId);
                return;
            }
            Long setmealId = orderDetail.getSetmealId();
            orderSetmealIdsList.add(setmealId);
        });

        //检查菜品当前的状态判断是否可以复用，把可用商品加入购物车
        List<Long> validDishIds = new ArrayList<>();
        List<Long> validSetmealIds = new ArrayList<>();
        if(!orderDishIdsList.isEmpty()){
            List<Dish> validDishList = dishMapper.getStatusDish(ENABLE);
            validDishIds = validDishList.stream().map(Dish::getId).collect(Collectors.toList());
        }

        if(!orderSetmealIdsList.isEmpty()){
            List<Setmeal> validSetmealList = setmealMapper.getStatusSetmeal(ENABLE);
            validSetmealIds = validSetmealList.stream().map(Setmeal::getId).collect(Collectors.toList());
        }

        final List<Long> finalValidDishIds = validDishIds;
        final List<Long> finalValidSetmealIds = validSetmealIds;

        List<OrderDetail> orderDetailListValid = orderDetailList.stream().filter(orderDetail -> {
            if(orderDetail.getDishId() != null && !finalValidDishIds.isEmpty()){
                return finalValidDishIds.contains(orderDetail.getDishId());
            }
            else if(orderDetail.getSetmealId() != null && !finalValidSetmealIds.isEmpty()){
                return finalValidSetmealIds.contains(orderDetail.getSetmealId());
            }
            return false;
        }).collect(Collectors.toList());

        log.info("可用商品：{}", orderDetailListValid);

        //把可用商品加入购物车
        List<ShoppingCart> shoppingCartListValid = orderDetailListValid.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);
            return shoppingCart;
        }).collect(Collectors.toList());

        log.info("再来一单商品：{}", shoppingCartListValid);

        //清空现有购物车
        shoppingcartMapper.cleanAllByUserId(userId);

        shoppingcartMapper.insertBatch(shoppingCartListValid);

    }

    @Override
    public PageResult adminPageQueryHistory(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        log.info("订单搜索：{}", ordersPageQueryDTO);

        Page<Orders> historyOrders = orderMapper.pageQueryHistory(ordersPageQueryDTO);


        //查询为空，则返回空
        if(historyOrders == null || historyOrders.getTotal() == 0){
            log.info("订单搜索结果为空");
            return new PageResult(0L, Collections.emptyList());
        }

        log.info("订单搜索结果：{}", historyOrders.getResult());

        //非空,准备封装VO
        List<OrderVO> orderVOList = new ArrayList<>();
        historyOrders.getResult().forEach(order -> {

            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(order.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderDetail);

            orderVO.setOrderDetailList(orderDetailList);
            orderVOList.add(orderVO);
        });

        PageResult pageResult = new PageResult(historyOrders.getTotal(), orderVOList);
        return pageResult;

    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.countByStatus(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(orderMapper.countByStatus(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS));
        log.info("订单统计：{}", orderStatisticsVO);
        return orderStatisticsVO;
    }

    @Override
    public void orderComfirmation(OrdersConfirmDTO ordersConfirmDTO) {
        Long orderId = ordersConfirmDTO.getId();

        orderStatusUpdate(Orders.CONFIRMED, orderId,null, null);

    }

    @Override
    public void orderRejection(OrdersRejectionDTO ordersRejectionDTO) {
        Long orderId = ordersRejectionDTO.getId();
        String rejectionReason = ordersRejectionDTO.getRejectionReason();
        orderStatusUpdate(Orders.CANCELLED, orderId, rejectionReason, null);

    }

    @Override
    public void orderCancellation(OrdersCancelDTO ordersCancelDTO) {
        Long orderId = ordersCancelDTO.getId();
        String cancelReason = ordersCancelDTO.getCancelReason();
        orderStatusUpdate(Orders.CANCELLED, orderId, null, cancelReason);
    }

    @Override
    public void orderDelivery(Long id) {
        orderStatusUpdate(Orders.DELIVERY_IN_PROGRESS,id,null,null);
    }

    @Override
    public void orderComplete(Long id) {
        orderStatusUpdate(Orders.COMPLETED,id,null,null);
    }

    @Override
    public void reminder(Long id) {
        //通过websocket给客户端发送消息
        Map map = new HashMap();
        map.put("type",2);
        map.put("orderId", id);
        map.put("content","用户催单");
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    private void orderStatusUpdate(Integer status, Long orderId, String rejectionReason, String cancelReason){
        Orders byId = orderMapper.getById(orderId);

        //订单不存在
        if(byId == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //拒单情况验证
        if(Objects.equals(status, Orders.CANCELLED) && rejectionReason != null &&!Objects.equals(byId.getStatus(), Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR + "无法拒单");
        }

        //派送情况验证
        if(Objects.equals(status, Orders.DELIVERY_IN_PROGRESS) && !Objects.equals(byId.getStatus(), Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR + "无法派送");
        }

        //完成情况验证
        if(Objects.equals(status, Orders.COMPLETED) && !Objects.equals(byId.getStatus(), Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR + "无法完成");
        }


        Orders orders = new Orders();
        orders.setId(orderId);
        orders.setStatus(status);
        if(Objects.equals(status, Orders.CANCELLED)){
            orders.setRejectionReason(rejectionReason);
            orders.setCancelReason(cancelReason);
            orders.setCancelTime(LocalDateTime.now());
        }
        if(Objects.equals(status, Orders.COMPLETED)){
            orders.setDeliveryTime(LocalDateTime.now());
        }
        orderMapper.update(orders);
    }

}
