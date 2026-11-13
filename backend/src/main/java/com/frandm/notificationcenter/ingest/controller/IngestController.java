package com.frandm.notificationcenter.ingest.controller;

import com.frandm.notificationcenter.ingest.dto.IngestNotificationRequest;
import com.frandm.notificationcenter.notification.dto.NotificationResponse;
import com.frandm.notificationcenter.notification.service.NotificationService;
import com.frandm.notificationcenter.security.model.ApiTokenAuthenticationToken;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class IngestController {

    private final NotificationService notificationService;

    public IngestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_notify') or hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> ingest(
            @Valid @RequestBody IngestNotificationRequest request,
            @RequestHeader(value = "X-Admin-Test-Source", required = false) String adminTestSource
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String source;

        if (auth instanceof ApiTokenAuthenticationToken tokenAuth) {
            source = tokenAuth.getSource();
        } else {
            source = (adminTestSource != null && !adminTestSource.isBlank())
                    ? adminTestSource.trim().toLowerCase()
                    : "web-admin";
        }

        NotificationResponse response = notificationService.ingest(source, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
