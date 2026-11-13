package com.frandm.notificationcenter.notification.dto;

import com.frandm.notificationcenter.notification.model.Notification;
import com.frandm.notificationcenter.notification.model.NotificationLevel;
import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        String source,
        String title,
        String body,
        NotificationLevel level,
        String url,
        String dedupKey,
        boolean read,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSource(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLevel(),
                notification.getUrl(),
                notification.getDedupKey(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
