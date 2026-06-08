package com.milktea.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.entity.MemberLevel;
import com.milktea.entity.PointsRecord;
import com.milktea.service.MemberService;
import com.milktea.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/member")
public class MemberController {

    private static final TreeMap<Integer, Integer> LEVEL_THRESHOLDS = new TreeMap<>();
    private static final Map<Integer, String> LEVEL_NAMES = Map.of(
            0, "普通会员",
            1, "银卡会员",
            2, "金卡会员",
            3, "黑卡会员"
    );
    private static final Map<Integer, BigDecimal> LEVEL_DISCOUNTS = Map.of(
            0, BigDecimal.ZERO,
            1, new BigDecimal("0.05"),
            2, new BigDecimal("0.10"),
            3, new BigDecimal("0.15")
    );

    static {
        LEVEL_THRESHOLDS.put(0, 0);
        LEVEL_THRESHOLDS.put(1, 500);
        LEVEL_THRESHOLDS.put(2, 1500);
        LEVEL_THRESHOLDS.put(3, 5000);
    }

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    @GetMapping("/level")
    public Result<Map<String, Object>> getMemberLevel() {
        Long userId = getCurrentUserId();
        MemberLevel memberLevel = memberService.getOrCreateMemberLevel(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("level", memberLevel.getLevel());
        data.put("levelName", LEVEL_NAMES.getOrDefault(memberLevel.getLevel(), "普通会员"));
        data.put("totalPoints", memberLevel.getTotalPoints());
        data.put("currentPoints", memberLevel.getCurrentPoints());
        data.put("discountRate", LEVEL_DISCOUNTS.getOrDefault(memberLevel.getLevel(), BigDecimal.ZERO));

        Map.Entry<Integer, Integer> nextLevel = LEVEL_THRESHOLDS.higherEntry(memberLevel.getLevel());
        if (nextLevel != null) {
            data.put("nextLevel", nextLevel.getKey());
            data.put("nextLevelName", LEVEL_NAMES.getOrDefault(nextLevel.getKey(), ""));
            data.put("nextLevelPoints", nextLevel.getValue());
            data.put("pointsToNextLevel", nextLevel.getValue() - memberLevel.getTotalPoints());
        } else {
            data.put("nextLevel", null);
            data.put("nextLevelName", null);
            data.put("nextLevelPoints", null);
            data.put("pointsToNextLevel", 0);
        }

        return Result.success(data);
    }

    @GetMapping("/points")
    public Result<Page<PointsRecord>> getPointsRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId();
        return Result.success(memberService.getPointsRecords(userId, page, pageSize));
    }

    @GetMapping("/discount")
    public Result<Map<String, Object>> calculateDiscount(@RequestParam BigDecimal amount) {
        Long userId = getCurrentUserId();
        BigDecimal discountRate = memberService.getDiscountRate(userId);
        BigDecimal discountAmount = memberService.calculateDiscount(userId, amount);

        Map<String, Object> data = new HashMap<>();
        data.put("discountRate", discountRate);
        data.put("discountAmount", discountAmount);
        data.put("originalAmount", amount);
        data.put("finalAmount", amount.subtract(discountAmount));
        return Result.success(data);
    }
}
