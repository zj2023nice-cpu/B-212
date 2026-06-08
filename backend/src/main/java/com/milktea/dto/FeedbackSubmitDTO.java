package com.milktea.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeedbackSubmitDTO {
    private Long orderId;
    private Long productId;
    private Integer rating;
    private String content;
    private List<String> images;
}
