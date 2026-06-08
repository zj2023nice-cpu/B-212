package com.milktea.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedbackVO {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer rating;
    private String content;
    private List<String> images;
    private String adminReply;
    private LocalDateTime createTime;
    private String nickname;
    private String avatarUrl;
}
