package com.frandm.notificationcenter.ingest.dto;

import com.frandm.notificationcenter.notification.model.NotificationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngestNotificationRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must not exceed 255 characters")
        String title,

        @NotBlank(message = "body is required")
        String body,

        NotificationLevel level,

        @Size(max = 2048, message = "url must not exceed 2048 characters")
        String url,

        @Size(max = 150, message = "dedupKey must not exceed 150 characters")
        String dedupKey
) {
}
