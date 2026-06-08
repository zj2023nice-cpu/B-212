package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milktea.common.Result;
import com.milktea.dto.FeedbackSubmitDTO;
import com.milktea.dto.FeedbackVO;
import com.milktea.entity.Feedback;
import com.milktea.entity.Order;
import com.milktea.entity.User;
import com.milktea.enums.OrderStatus;
import com.milktea.mapper.FeedbackMapper;
import com.milktea.mapper.OrderItemMapper;
import com.milktea.mapper.OrderMapper;
import com.milktea.service.UserService;
import com.milktea.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_IMAGES = 3;

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

    private List<String> cleanImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        return images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(image -> !image.isEmpty())
                .collect(Collectors.toList());
    }

    private String serializeImages(List<String> images) {
        try {
            return objectMapper.writeValueAsString(cleanImages(images));
        } catch (Exception e) {
            throw new RuntimeException("评价图片序列化失败", e);
        }
    }

    private List<String> parseImages(String rawImages) {
        if (rawImages == null || rawImages.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String trimmedImages = rawImages.trim();
        try {
            if (trimmedImages.startsWith("[")) {
                List<String> parsedImages = objectMapper.readValue(trimmedImages, new TypeReference<List<String>>() {});
                return cleanImages(parsedImages);
            }
        } catch (Exception e) {
            logger.warn("解析评价图片JSON失败，回退到旧格式解析: {}", trimmedImages, e);
        }

        return Arrays.stream(trimmedImages.split(","))
                .map(String::trim)
                .filter(image -> !image.isEmpty())
                .collect(Collectors.toList());
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
            vo.setImages(parseImages(fb.getImages()));
            vo.setAdminReply(fb.getAdminReply());

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
    public Result<String> submitFeedbacks(@RequestBody List<FeedbackSubmitDTO> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return Result.error("评价列表不能为空");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        Long orderId = feedbacks.get(0).getOrderId();
        if (orderId == null) {
            return Result.error("订单ID不能为空");
        }

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

        for (FeedbackSubmitDTO feedbackDTO : feedbacks) {
            if (!orderId.equals(feedbackDTO.getOrderId())) {
                return Result.error("评价列表中的订单ID不一致");
            }
            if (feedbackDTO.getRating() == null || feedbackDTO.getRating() < 1 || feedbackDTO.getRating() > 5) {
                return Result.error("评分必须在1-5之间");
            }
            if (feedbackDTO.getProductId() == null) {
                return Result.error("商品ID不能为空");
            }

            List<String> images = cleanImages(feedbackDTO.getImages());
            if (images.size() > MAX_IMAGES) {
                return Result.error("评价图片最多上传3张");
            }

            Feedback feedback = new Feedback();
            feedback.setOrderId(orderId);
            feedback.setUserId(userId);
            feedback.setProductId(feedbackDTO.getProductId());
            feedback.setRating(feedbackDTO.getRating());
            feedback.setContent(feedbackDTO.getContent());
            feedback.setImages(serializeImages(images));
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
            @RequestParam(required = false) Boolean hasImage,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        LambdaQueryWrapper<Feedback> query = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getProductId, productId);

        if (rating != null) {
            query.eq(Feedback::getRating, rating);
        }

        if (hasImage != null && hasImage) {
            query.isNotNull(Feedback::getImages)
                    .ne(Feedback::getImages, "")
                    .ne(Feedback::getImages, "[]");
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
        Long userId = SecurityUtils.getCurrentUserId();
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

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> replyFeedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reply = body.get("reply");
        if (reply == null || reply.trim().isEmpty()) {
            return Result.error("回复内容不能为空");
        }

        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            return Result.error("评价不存在");
        }

        feedback.setAdminReply(reply.trim());
        feedbackMapper.updateById(feedback);

        logger.info("管理员回复评价: feedbackId={}", id);
        return Result.success("回复成功");
    }
}
