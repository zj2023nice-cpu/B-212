package com.milktea.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.dto.OrderExportDTO;
import com.milktea.dto.OrderExportVO;
import com.milktea.entity.ExportAuditLog;
import com.milktea.entity.Order;
import com.milktea.entity.OrderItem;
import com.milktea.entity.User;
import com.milktea.enums.OrderStatus;
import com.milktea.mapper.ExportAuditLogMapper;
import com.milktea.mapper.OrderItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.mapper.UserMapper;
import com.milktea.service.OrderExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderExportServiceImpl implements OrderExportService {

    private static final Logger logger = LoggerFactory.getLogger(OrderExportServiceImpl.class);
    private static final int BATCH_SIZE = 500;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExportAuditLogMapper exportAuditLogMapper;

    @Override
    public void exportOrders(OrderExportDTO dto, Long operatorId, String operatorName, HttpServletResponse response) {
        int totalCount = 0;
        try {
            LambdaQueryWrapper<Order> queryWrapper = buildQueryWrapper(dto);
            long totalRecords = orderMapper.selectCount(queryWrapper);

            if (totalRecords == 0) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"没有符合条件的数据可导出\"}");
                return;
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("订单数据_" + LocalDate.now().format(DATE_FMT), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), OrderExportVO.class).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("订单数据").build();

            try {
                long totalPages = (totalRecords + BATCH_SIZE - 1) / BATCH_SIZE;
                for (long currentPage = 1; currentPage <= totalPages; currentPage++) {
                    Page<Order> page = new Page<>(currentPage, BATCH_SIZE, false);
                    Page<Order> orderPage = orderMapper.selectPage(page, buildQueryWrapper(dto));
                    List<Order> batchOrders = orderPage.getRecords();
                    if (batchOrders.isEmpty()) {
                        continue;
                    }

                    Map<Long, User> userMap = getUserMap(batchOrders);
                    Map<Long, List<OrderItem>> orderItemMap = getOrderItemMap(batchOrders);

                    List<OrderExportVO> voList = new ArrayList<>(batchOrders.size());
                    for (Order order : batchOrders) {
                        voList.add(convertToVO(order, userMap, orderItemMap));
                    }

                    excelWriter.write(voList, writeSheet);
                    totalCount += voList.size();
                    logger.info("订单导出分批写入进度: 第{}/{}页, 当前批次: {}条, 累计: {}/{}",
                            currentPage, totalPages, voList.size(), totalCount, totalRecords);
                }
            } finally {
                excelWriter.finish();
            }

            saveAuditLog(dto, operatorId, operatorName, totalCount);
            logger.info("订单导出完成, 操作人: {}, 导出数量: {}", operatorName, totalCount);

        } catch (IOException e) {
            logger.error("订单导出IO异常: {}", e.getMessage(), e);
            throw new RuntimeException("导出文件写入失败", e);
        }
    }

    private LambdaQueryWrapper<Order> buildQueryWrapper(OrderExportDTO dto) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            LocalDateTime startDateTime = LocalDate.parse(dto.getStartDate()).atStartOfDay();
            wrapper.ge(Order::getCreateTime, startDateTime);
        }

        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            LocalDateTime endDateTime = LocalDate.parse(dto.getEndDate()).atTime(LocalTime.MAX);
            wrapper.le(Order::getCreateTime, endDateTime);
        }

        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(dto.getStatus());
                wrapper.eq(Order::getStatus, orderStatus);
            } catch (IllegalArgumentException e) {
                // ignore invalid status
            }
        }

        wrapper.orderByDesc(Order::getCreateTime)
                .orderByDesc(Order::getId);
        return wrapper;
    }

    private Map<Long, User> getUserMap(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        Set<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, List<OrderItem>> getOrderItemMap(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        return allItems.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private OrderExportVO convertToVO(Order order, Map<Long, User> userMap, Map<Long, List<OrderItem>> orderItemMap) {
        OrderExportVO vo = new OrderExportVO();
        vo.setOrderSn(order.getOrderSn());
        vo.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().format(DATE_TIME_FMT) : "");

        User user = userMap.get(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname() != null ? user.getNickname() : "");
            vo.setPhone(user.getPhone() != null ? user.getPhone() : "");
        }

        List<OrderItem> items = orderItemMap.getOrDefault(order.getId(), List.of());
        String productDetail = items.stream()
                .map(item -> item.getProductName() + " x" + item.getQuantity())
                .collect(Collectors.joining("; "));
        vo.setProductDetail(productDetail);

        vo.setTotalAmount(order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0.00");
        vo.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount().toPlainString() : "0.00");
        vo.setPayAmount(order.getPayAmount() != null ? order.getPayAmount().toPlainString() : "0.00");

        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setContactName(order.getAddressContactName() != null ? order.getAddressContactName() : "");
        vo.setContactPhone(order.getAddressPhone() != null ? order.getAddressPhone() : "");
        vo.setAddressFull(order.getAddressFull() != null ? order.getAddressFull() : "");
        vo.setRemark(order.getRemark() != null ? order.getRemark() : "");

        return vo;
    }

    private String getStatusText(OrderStatus status) {
        if (status == null) return "未知";
        return status.getDescription();
    }

    private void saveAuditLog(OrderExportDTO dto, Long operatorId, String operatorName, int exportCount) {
        try {
            ExportAuditLog auditLog = new ExportAuditLog();
            auditLog.setOperatorId(operatorId);
            auditLog.setOperatorName(operatorName);
            auditLog.setExportType("ORDER_EXPORT");
            auditLog.setFilterStartDate(dto.getStartDate());
            auditLog.setFilterEndDate(dto.getEndDate());
            auditLog.setFilterStatus(dto.getStatus() != null && !dto.getStatus().isEmpty()
                    ? OrderStatus.valueOf(dto.getStatus()).getDescription() : "全部");
            auditLog.setExportCount(exportCount);
            exportAuditLogMapper.insert(auditLog);
        } catch (Exception e) {
            logger.error("保存导出审计日志失败: {}", e.getMessage(), e);
        }
    }
}
