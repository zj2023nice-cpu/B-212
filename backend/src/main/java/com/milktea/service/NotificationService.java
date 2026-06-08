package com.milktea.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.milktea.entity.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {
    void sendNotification(Long userId, String title, String content, String type, Long businessId);
    Page<Notification> getUserNotifications(Long userId, String type, Integer page, Integer pageSize);
    List<Notification> getRecentNotifications(Long userId, Integer limit);
    Long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAsReadBatch(List<Long> ids, Long userId);
    void markAllAsRead(Long userId);
    void deleteNotification(Long notificationId, Long userId);
}
