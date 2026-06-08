package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.common.Result;
import com.milktea.dto.FeedbackVO;
import com.milktea.entity.Feedback;
import com.milktea.entity.Order;
import com.milktea.entity.User;
import com.milktea.enums.OrderStatus;
import com.milktea.mapper.FeedbackMapper;
import com.milktea.mapper.OrderItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private UserService userService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    private Map<Long, User> batchGetUsers(List<Feedback> feedbacks) {
        Set<Long> userIds = feedbacks.stream()
                .map(Feedback::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userService.listByIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private List<FeedbackVO> convertToVOList(List<Feedback> feedbacks, Map<Long, User> userMap) {
        return feedbacks.stream().map(fb -> {
            FeedbackVO vo = new FeedbackVO();
            vo.setId(fb.getId());
            vo.setOrderId(fb.getOrderId());
            vo.setUserId(fb.getUserId());
            vo.setProductId(fb.getProductId());
            vo.setRating(fb.getRating());
            vo.setContent(fb.getContent());
            vo.setCreateTime(fb.getCreateTime());

            if (fb.getImages() != null && !fb.getImages().isEmpty()) {
                try {
                    vo.setImages(Arrays.asList(fb.getImages().split(",")));
                } catch (Exception e) {
                    vo.setImages(Collections.emptyList());
                }
            } else {
                vo.setImages(Collections.emptyList());
            }

            User user = userMap.get(fb.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setAvatarUrl(user.getAvatarUrl());
            } else {
                vo.setNickname("匿名用户");
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @PostMapping
    @Transactional
    public Result<String> submitFeedbacks(@RequestBody List<Feedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return Result.error("评价列表不能为空");
        }

        Long userId = getCurrentUserId();
        Long orderId = feedbacks.get(0).getOrderId();

        Order existingOrder = orderMapper.selectById(orderId);
        if (existingOrder == null) {
            return Result.error("Order not found");
        }

        if (!existingOrder.getUserId().equals(userId)) {
            return Result.error("Not authorized to submit feedback for this order");
        }

        if (existingOrder.getStatus() != OrderStatus.COMPLETED) {
            return Result.error("Only completed orders can be reviewed");
        }

        LambdaQueryWrapper<Feedback> existCheck = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getOrderId, orderId)
                .eq(Feedback::getUserId, userId);
        Long existCount = feedbackMapper.selectCount(existCheck);
        if (existCount > 0) {
            return Result.error("该订单已评价，不能重复评价");
        }

        for (Feedback feedback : feedbacks) {
            if (feedback.getRating() == null || feedback.getRating() < 1 || feedback.getRating() > 5) {
                return Result.error("评分必须在1-5之间");
            }
            if (feedback.getProductId() == null) {
                return Result.error("商品ID不能为空");
            }
            feedback.setOrderId(orderId);
            feedback.setUserId(userId);
            feedbackMapper.insert(feedback);
        }

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.REVIEWED);
        orderMapper.updateById(order);

        logger.info("订单评价成功: orderId={}, userId={}, 评价商品数={}", orderId, userId, feedbacks.size());
        return Result.success("Feedback submitted");
    }

    @GetMapping("/product/{productId}")
    public Result<List<FeedbackVO>> getProductFeedbacks(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        LambdaQueryWrapper<Feedback> query = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getProductId, productId);

        if (rating != null) {
            query.eq(Feedback::getRating, rating);
        }

        if ("asc".equalsIgnoreCase(sortOrder)) {
            query.orderByAsc(Feedback::getCreateTime);
        } else {
            query.orderByDesc(Feedback::getCreateTime);
        }

        List<Feedback> feedbacks = feedbackMapper.selectList(query);
        Map<Long, User> userMap = batchGetUsers(feedbacks);
        return Result.success(convertToVOList(feedbacks, userMap));
    }

    @GetMapping("/order/{orderId}")
    public Result<List<FeedbackVO>> getOrderFeedbacks(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("Order not found");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("Not authorized");
        }

        List<Feedback> feedbacks = feedbackMapper.selectList(
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getOrderId, orderId)
                        .orderByDesc(Feedback::getCreateTime));
        Map<Long, User> userMap = batchGetUsers(feedbacks);
        return Result.success(convertToVOList(feedbacks, userMap));
    }

    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            String imageUrl = "/uploads/" + filename;
            logger.info("评价图片上传成功: {}", imageUrl);
            return Result.success(imageUrl);
        } catch (IOException e) {
            logger.error("图片上传失败: {}", e.getMessage());
            return Result.error("图片上传失败");
        }
    }
}
