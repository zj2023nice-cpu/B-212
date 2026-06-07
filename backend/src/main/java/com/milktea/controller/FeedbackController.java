package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.common.Result;
import com.milktea.entity.Feedback;
import com.milktea.entity.Order;
import com.milktea.mapper.FeedbackMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        // 向后兼容：如果 details 不是 Long 类型（旧的 Token），则查询数据库
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    @PostMapping
    @Transactional
    public Result<String> submitFeedback(@RequestBody Feedback feedback) {
        Long userId = getCurrentUserId();
        
        // 验证订单是否属于当前用户
        Order existingOrder = orderMapper.selectById(feedback.getOrderId());
        if (existingOrder == null) {
            return Result.error("Order not found");
        }
        
        if (!existingOrder.getUserId().equals(userId)) {
            return Result.error("Not authorized to submit feedback for this order");
        }
        
        feedback.setUserId(userId);
        feedbackMapper.insert(feedback);

        // 更新订单状态为已评价
        Order order = new Order();
        order.setId(feedback.getOrderId());
        order.setStatus(5);
        orderMapper.updateById(order);

        return Result.success("Feedback submitted");
    }

    @GetMapping("/product/{productId}")
    public Result<List<Feedback>> getProductFeedbacks(@PathVariable Long productId) {
        // 简单逻辑：这里通过 order_items 关联可能更复杂，简化为按订单查询或全部展示
        return Result.success(feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>().orderByDesc(Feedback::getCreateTime)));
    }
}
