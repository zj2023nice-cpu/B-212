package com.milktea.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.entity.Notification;
import com.milktea.service.NotificationService;
import com.milktea.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

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

    @GetMapping
    public Result<Page<Notification>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.getUserNotifications(userId, type, page, pageSize));
    }

    @GetMapping("/recent")
    public Result<List<Notification>> recent(@RequestParam(defaultValue = "5") Integer limit) {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.getRecentNotifications(userId, limit));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success("Marked as read");
    }

    @PutMapping("/read-all")
    public Result<String> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("All marked as read");
    }

    @PutMapping("/batch-read")
    public Result<String> batchMarkAsRead(@RequestBody List<Long> ids) {
        Long userId = getCurrentUserId();
        notificationService.markAsReadBatch(ids, userId);
        return Result.success("Batch marked as read");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return Result.success("Deleted");
    }
}
