package com.milktea.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.entity.Notification;
import com.milktea.service.NotificationService;
import com.milktea.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public Result<Page<Notification>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.getUserNotifications(userId, type, page, pageSize));
    }

    @GetMapping("/recent")
    public Result<List<Notification>> recent(@RequestParam(defaultValue = "5") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.getRecentNotifications(userId, limit));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success("Marked as read");
    }

    @PutMapping("/read-all")
    public Result<String> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("All marked as read");
    }

    @PutMapping("/batch-read")
    public Result<String> batchMarkAsRead(@RequestBody List<Long> ids) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsReadBatch(ids, userId);
        return Result.success("Batch marked as read");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return Result.success("Deleted");
    }
}
