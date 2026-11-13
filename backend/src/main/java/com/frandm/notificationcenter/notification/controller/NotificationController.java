package com.frandm.notificationcenter.notification.controller;

import com.frandm.notificationcenter.notification.dto.NotificationResponse;
import com.frandm.notificationcenter.notification.dto.UnreadCountResponse;
import com.frandm.notificationcenter.notification.model.NotificationLevel;
import com.frandm.notificationcenter.notification.service.NotificationService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_read') or hasRole('ADMIN')")
    public Page<NotificationResponse> listNotifications(
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "read", required = false) Boolean read,
            @RequestParam(name = "level", required = false) NotificationLevel level,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size
    ) {
        int boundedSize = Math.clamp(size, 1, 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), boundedSize, Sort.by("createdAt").descending());
        return notificationService.getNotifications(source, read, level, pageable);
    }

    @GetMapping("/sources")
    @PreAuthorize("hasAuthority('SCOPE_read') or hasRole('ADMIN')")
    public List<String> listSources() {
        return notificationService.getSources();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('SCOPE_read') or hasRole('ADMIN')")
    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(notificationService.getUnreadCount());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        return notificationService.markRead(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        boolean deleted = notificationService.deleteNotification(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
