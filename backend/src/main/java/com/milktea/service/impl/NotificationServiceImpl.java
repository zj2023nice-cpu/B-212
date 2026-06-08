package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.milktea.entity.Notification;
import com.milktea.mapper.NotificationMapper;
import com.milktea.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    public void sendNotification(Long userId, String title, String content, String type, Long businessId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setBusinessId(businessId);
        save(notification);
        logger.info("发送通知: userId={}, type={}, title={}", userId, type, title);
    }

    @Override
    public Page<Notification> getUserNotifications(Long userId, String type, Integer page, Integer pageSize) {
        Page<Notification> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            query.eq(Notification::getType, type);
        }
        query.orderByDesc(Notification::getCreateTime);
        return page(pageParam, query);
    }

    @Override
    public List<Notification> getRecentNotifications(Long userId, Integer limit) {
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime)
                .last("LIMIT " + limit);
        return list(query);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1);
        update(updateWrapper);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        update(updateWrapper);
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        remove(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId));
    }
}
