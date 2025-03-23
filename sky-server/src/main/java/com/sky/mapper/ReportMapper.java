package com.sky.mapper;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.vo.TurnoverReportVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    Double getTurnoverByDate(Map map);

    Integer getNewUserByDate(Map map);

    Integer getTotalUserByDate(Map map);

    Integer getOrderCountByMap(Map map);

    List<Integer> getOrderIdByDate(Map map);

    List<OrderDetail> getOrderDetailByOrderId(List<Integer> orderIdByDate);

    List<GoodsSalesDTO> getGoodsSales(LocalDate begin, LocalDate end);
}
