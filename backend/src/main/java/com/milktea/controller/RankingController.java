package com.milktea.controller;

import com.milktea.common.Result;
import com.milktea.dto.HotProductVO;
import com.milktea.service.RankingService;
import com.milktea.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    @Autowired
    private UserService userService;

    @GetMapping("/hot")
    public Result<List<HotProductVO>> getHotRanking(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "10") Integer limit) {
        if (days != 7 && days != 30) {
            days = 7;
        }
        if (limit < 1 || limit > 50) {
            limit = 10;
        }
        return Result.success(rankingService.getHotRanking(days, categoryId, limit));
    }

    @GetMapping("/recommend")
    public Result<List<HotProductVO>> getRecommendation(
            @RequestParam(defaultValue = "6") Integer limit) {
        if (limit < 1 || limit > 50) {
            limit = 6;
        }
        Long userId = getCurrentUserId();
        return Result.success(rankingService.getRecommendation(userId, limit));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = authentication.getName();
        if ("anonymousUser".equals(username)) {
            return null;
        }
        try {
            return userService.getByUsername(username).getId();
        } catch (Exception e) {
            return null;
        }
    }
}
