package com.frandm.notificationcenter.notification.service;

import com.frandm.notificationcenter.ingest.dto.IngestNotificationRequest;
import com.frandm.notificationcenter.notification.dto.NotificationResponse;
import com.frandm.notificationcenter.notification.model.Notification;
import com.frandm.notificationcenter.notification.model.NotificationLevel;
import com.frandm.notificationcenter.notification.repository.NotificationRepository;
import com.frandm.notificationcenter.push.service.PushNotificationService;
import com.frandm.notificationcenter.stream.service.SseEmitterService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;
    private final PushNotificationService pushService;

    public NotificationService(
            NotificationRepository notificationRepository,
            SseEmitterService sseEmitterService,
            PushNotificationService pushService
    ) {
        this.notificationRepository = notificationRepository;
        this.sseEmitterService = sseEmitterService;
        this.pushService = pushService;
    }

    @Transactional
    public NotificationResponse ingest(String source, IngestNotificationRequest request) {
        Notification notification;
        String dedupKey = request.dedupKey() != null && !request.dedupKey().isBlank()
                ? request.dedupKey().trim()
                : null;

        if (dedupKey != null) {
            Optional<Notification> existingOpt = notificationRepository
                    .findFirstByDedupKeyAndReadFalseOrderByCreatedAtDesc(dedupKey);

            if (existingOpt.isPresent()) {
                notification = existingOpt.get();
                notification.setSource(source);
                notification.setTitle(request.title().trim());
                notification.setBody(request.body().trim());
                notification.setLevel(request.level() != null ? request.level() : NotificationLevel.INFO);
                notification.setUrl(request.url() != null ? request.url().trim() : null);
                notification.setCreatedAt(OffsetDateTime.now());
            } else {
                notification = buildNewNotification(source, request, dedupKey);
            }
        } else {
            notification = buildNewNotification(source, request, null);
        }

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.from(saved);

        sseEmitterService.broadcast("notification:new", response);
        sseEmitterService.broadcast("unread-count", notificationRepository.countByReadFalse());

        pushService.dispatchPush(saved);

        return response;
    }

    private Notification buildNewNotification(String source, IngestNotificationRequest request, String dedupKey) {
        Notification notification = new Notification();
        notification.setSource(source);
        notification.setTitle(request.title().trim());
        notification.setBody(request.body().trim());
        notification.setLevel(request.level() != null ? request.level() : NotificationLevel.INFO);
        notification.setUrl(request.url() != null ? request.url().trim() : null);
        notification.setDedupKey(dedupKey);
        notification.setRead(false);
        notification.setCreatedAt(OffsetDateTime.now());
        return notification;
    }

    public Page<NotificationResponse> getNotifications(
            String source,
            Boolean isRead,
            NotificationLevel level,
            Pageable pageable
    ) {
        String normalizedSource = source != null && !source.isBlank() && !"all".equalsIgnoreCase(source.trim())
                ? source.trim().toLowerCase()
                : null;

        return notificationRepository.findFiltered(normalizedSource, isRead, level, pageable)
                .map(NotificationResponse::from);
    }

    public List<String> getSources() {
        return notificationRepository.findDistinctSources();
    }

    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    @Transactional
    public Optional<NotificationResponse> markRead(Long id) {
        return notificationRepository.findById(id).map(notification -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                notificationRepository.save(notification);
                sseEmitterService.broadcast("notification:read", Map.of("id", id, "read", true));
                sseEmitterService.broadcast("unread-count", notificationRepository.countByReadFalse());
            }
            return NotificationResponse.from(notification);
        });
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllAsRead();
        sseEmitterService.broadcast("notification:read-all", Map.of("allRead", true));
        sseEmitterService.broadcast("unread-count", 0L);
    }

    @Transactional
    public boolean deleteNotification(Long id) {
        return notificationRepository.findById(id).map(notification -> {
            notificationRepository.delete(notification);
            sseEmitterService.broadcast("notification:deleted", Map.of("id", id));
            sseEmitterService.broadcast("unread-count", notificationRepository.countByReadFalse());
            return true;
        }).orElse(false);
    }
}
