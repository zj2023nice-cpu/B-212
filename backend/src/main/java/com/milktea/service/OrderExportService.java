package com.milktea.service;

import com.milktea.dto.OrderExportDTO;
import jakarta.servlet.http.HttpServletResponse;

public interface OrderExportService {
    void exportOrders(OrderExportDTO dto, Long operatorId, String operatorName, HttpServletResponse response);
}
