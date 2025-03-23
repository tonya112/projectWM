package com.sky.service.impl;

import com.alibaba.druid.sql.visitor.functions.Locate;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    private WorkspaceService workspaceService;


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

    @Override
    public void exportBusinessReport(HttpServletResponse response) {
        //查询数据库获取营业数据
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(beginDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX));

        //把数据写入excel
        //基于模板文件创建新的excel文件
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(resourceAsStream);

            //填充数据

            //填充时间数据
            String dateStr = "日期" + beginDate + "至" + endDate;
            workbook.getSheet("Sheet1").getRow(2).getCell(1).setCellValue(dateStr);

            //填充
            XSSFRow row = workbook.getSheet("Sheet1").getRow(3);
            //营业额
            row.getCell(2).setCellValue(businessData.getTurnover());
            //订单完成率
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            //新增用户数
            row.getCell(6).setCellValue(businessData.getNewUsers());

            //
            row = workbook.getSheet("Sheet1").getRow(4);
            //有效订单数
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            //平均客单价
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            //填充明细
            for(int i = 0;i < 30; i++){
                LocalDate date = beginDate.plusDays(i);
                BusinessDataVO dailyBusinessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = workbook.getSheet("Sheet1").getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(dailyBusinessData.getTurnover());
                row.getCell(3).setCellValue(dailyBusinessData.getValidOrderCount());
                row.getCell(4).setCellValue(dailyBusinessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(dailyBusinessData.getUnitPrice());
                row.getCell(6).setCellValue(dailyBusinessData.getNewUsers());
            }

            //通过输出流下载excel到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);

            outputStream.close();
            workbook.close();




        } catch (IOException e) {
            throw new RuntimeException(e);
        }


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
