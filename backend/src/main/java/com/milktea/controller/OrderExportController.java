package com.milktea.controller;

import com.milktea.dto.OrderExportDTO;
import com.milktea.entity.User;
import com.milktea.service.OrderExportService;
import com.milktea.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderExportController {

    private static final Logger logger = LoggerFactory.getLogger(OrderExportController.class);

    @Autowired
    private OrderExportService orderExportService;

    @Autowired
    private UserService userService;

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportOrders(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status,
            Authentication authentication,
            HttpServletResponse response) {

        String username = authentication.getName();
        User user = userService.getByUsername(username);
        Long operatorId = user.getId();
        String operatorName = user.getNickname() != null ? user.getNickname() : user.getUsername();

        logger.info("管理员[{}]请求导出订单, 筛选条件: startDate={}, endDate={}, status={}",
                operatorName, startDate, endDate, status);

        OrderExportDTO dto = new OrderExportDTO();
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setStatus(status);

        orderExportService.exportOrders(dto, operatorId, operatorName, response);
    }
}
