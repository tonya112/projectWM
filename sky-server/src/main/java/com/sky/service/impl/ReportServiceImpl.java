package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;


    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //存放begin到end范围内的日期
        List<LocalDate> dateList = getLocalDates(begin, end);

        String dateListStr = dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));

        List<Double> turnoverList = new ArrayList<>();
        dateList.forEach(date -> {
            Map map = new HashMap();
            map.put("date", date);
            map.put("status", Orders.COMPLETED);
            Double turnover = reportMapper.getTurnoverByDate(map);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        });

        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();

        return turnoverReportVO;
    }



    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getLocalDates(begin, end);

        String dateListStr = dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));


        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        dateList.forEach(date -> {
            Map map = new HashMap();
            map.put("date", date);
            Integer newUser = reportMapper.getNewUserByDate(map);
            Integer totalUser = reportMapper.getTotalUserByDate(map);
            newUserList.add(newUser == null ? 0 : newUser);
            totalUserList.add(totalUser == null ? 0 : totalUser);
        });
        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .totalUserList(totalUserList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();


        return userReportVO;
    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getLocalDates(begin, end);
        String dateListStr = dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        dateList.forEach(date -> {
            Map map = new HashMap();
            map.put("date",date);
            Integer orderCount = reportMapper.getOrderCountByMap(map);
            orderCountList.add(orderCount == null ? 0 : orderCount);
            map.put("status",Orders.COMPLETED);
            Integer validOrderCount = reportMapper.getOrderCountByMap(map);
            validOrderCountList.add(validOrderCount == null ? 0 : validOrderCount);
        });
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);
        Double orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(orderCountList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .validOrderCountList(validOrderCountList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        /*Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status", Orders.COMPLETED);
        List<Integer> orderIdByDate = reportMapper.getOrderIdByDate(map);
        if (orderIdByDate == null || orderIdByDate.isEmpty()) {
            return SalesTop10ReportVO.builder().build();
        }

        List<OrderDetail> orderDetailList = reportMapper.getOrderDetailByOrderId(orderIdByDate);

        Map<String, Integer> Collection = orderDetailList.stream().collect(Collectors.groupingBy(OrderDetail::getName, Collectors.summingInt(OrderDetail::getNumber)));
        Map<String, Integer> top10Collection = Collection.entrySet().stream()
                .sorted((entry1,entry2)-> Integer.compare(entry2.getValue(),entry1.getValue()))
                .limit(10).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String nameList = top10Collection.keySet().stream().collect(Collectors.joining(","));
        String numberList = top10Collection.values().stream().map(String::valueOf).collect(Collectors.joining(","));

        SalesTop10ReportVO salesTop10ReportVO = SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();*/
        List<GoodsSalesDTO> goodsSalesDTOs = reportMapper.getGoodsSales(begin,end);
        String nameList = goodsSalesDTOs.stream().map(GoodsSalesDTO::getName).collect(Collectors.joining(","));
        String numberList = goodsSalesDTOs.stream().map(goodsSalesDTO -> String.valueOf(goodsSalesDTO.getNumber())).collect(Collectors.joining(","));
        SalesTop10ReportVO salesTop10ReportVO = SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
        return salesTop10ReportVO;
    }

    private static List<LocalDate> getLocalDates(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        while (!begin.equals(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);
        return dateList;
    }


}
