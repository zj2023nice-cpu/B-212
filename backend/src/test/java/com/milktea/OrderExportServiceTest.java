package com.milktea;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.dto.OrderExportDTO;
import com.milktea.entity.ExportAuditLog;
import com.milktea.entity.Order;
import com.milktea.entity.OrderItem;
import com.milktea.entity.User;
import com.milktea.enums.OrderStatus;
import com.milktea.mapper.ExportAuditLogMapper;
import com.milktea.mapper.OrderItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.mapper.UserMapper;
import com.milktea.service.impl.OrderExportServiceImpl;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExportService 测试")
class OrderExportServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ExportAuditLogMapper exportAuditLogMapper;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private OrderExportServiceImpl orderExportService;

    @Test
    @DisplayName("测试 exportOrders - 按页读取并导出成功")
    void testExportOrders_BatchPagingSuccess() throws IOException {
        OrderExportDTO dto = new OrderExportDTO();
        dto.setStartDate("2026-06-01");
        dto.setEndDate("2026-06-08");
        dto.setStatus("PREPARING");

        Order order1 = buildOrder(1L, "SN001", 101L, LocalDateTime.of(2026, 6, 8, 10, 0, 0));
        Order order2 = buildOrder(2L, "SN002", 102L, LocalDateTime.of(2026, 6, 8, 9, 59, 0));
        Order order3 = buildOrder(3L, "SN003", 103L, LocalDateTime.of(2026, 6, 8, 9, 58, 0));

        User user1 = buildUser(101L, "user101", "用户101", "13800000001");
        User user2 = buildUser(102L, "user102", "用户102", "13800000002");
        User user3 = buildUser(103L, "user103", "用户103", "13800000003");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream(output));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1000L);
        when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<Order> requestPage = invocation.getArgument(0);
                    Page<Order> result = new Page<>(requestPage.getCurrent(), requestPage.getSize(), false);
                    if (requestPage.getCurrent() == 1L) {
                        result.setRecords(List.of(order1, order2));
                    } else if (requestPage.getCurrent() == 2L) {
                        result.setRecords(List.of(order3));
                    } else {
                        result.setRecords(List.of());
                    }
                    return result;
                });
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user1, user2), List.of(user3));
        when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(
                        buildOrderItem(1L, "珍珠奶茶", 2),
                        buildOrderItem(2L, "椰果奶茶", 1)
                ),
                List.of(buildOrderItem(3L, "茉莉奶绿", 3))
        );

        orderExportService.exportOrders(dto, 9001L, "管理员A", response);

        verify(orderMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        verify(orderMapper, times(2)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(orderMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(userMapper, times(2)).selectBatchIds(any());
        verify(orderItemMapper, times(2)).selectList(any(LambdaQueryWrapper.class));
        verify(exportAuditLogMapper, times(1)).insert(argThat(log ->
                log.getOperatorId().equals(9001L)
                        && "管理员A".equals(log.getOperatorName())
                        && "ORDER_EXPORT".equals(log.getExportType())
                        && "2026-06-01".equals(log.getFilterStartDate())
                        && "2026-06-08".equals(log.getFilterEndDate())
                        && "制作中".equals(log.getFilterStatus())
                        && log.getExportCount().equals(3)
        ));
        assertTrue(output.size() > 0);
    }

    @Test
    @DisplayName("测试 exportOrders - 无数据时返回JSON错误")
    void testExportOrders_NoDataReturnsJson() throws IOException {
        OrderExportDTO dto = new OrderExportDTO();
        StringWriter stringWriter = new StringWriter();

        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        orderExportService.exportOrders(dto, 1L, "管理员", response);

        verify(response).setContentType("application/json;charset=UTF-8");
        verify(orderMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(userMapper, never()).selectBatchIds(any());
        verifyNoInteractions(orderItemMapper, exportAuditLogMapper);
        assertTrue(stringWriter.toString().contains("没有符合条件的数据可导出"));
    }

    private Order buildOrder(Long id, String orderSn, Long userId, LocalDateTime createTime) {
        Order order = new Order();
        order.setId(id);
        order.setOrderSn(orderSn);
        order.setUserId(userId);
        order.setCreateTime(createTime);
        order.setStatus(OrderStatus.PREPARING);
        order.setTotalAmount(new BigDecimal("30.00"));
        order.setDiscountAmount(new BigDecimal("3.00"));
        order.setPayAmount(new BigDecimal("27.00"));
        order.setAddressContactName("张三");
        order.setAddressPhone("13800138000");
        order.setAddressFull("测试地址");
        order.setRemark("少冰");
        return order;
    }

    private User buildUser(Long id, String username, String nickname, String phone) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPhone(phone);
        return user;
    }

    private OrderItem buildOrderItem(Long orderId, String productName, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductName(productName);
        item.setQuantity(quantity);
        item.setProductPrice(new BigDecimal("15.00"));
        return item;
    }

    private static class TestServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream output;

        private TestServletOutputStream(ByteArrayOutputStream output) {
            this.output = output;
        }

        @Override
        public void write(int b) {
            output.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
    }
}
