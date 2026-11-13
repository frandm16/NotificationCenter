package com.frandm.notificationcenter.push.service;

import com.frandm.notificationcenter.notification.model.Notification;
import com.frandm.notificationcenter.push.dto.PushSubscriptionRequest;
import com.frandm.notificationcenter.push.dto.VapidKeyResponse;
import com.frandm.notificationcenter.push.model.PushSubscription;
import com.frandm.notificationcenter.push.repository.PushSubscriptionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final String vapidPublicKey;
    private final String vapidPrivateKey;
    private final String vapidSubject;

    public PushNotificationService(
            PushSubscriptionRepository subscriptionRepository,
            @Value("${app.push.vapid-public-key:}") String vapidPublicKey,
            @Value("${app.push.vapid-private-key:}") String vapidPrivateKey,
            @Value("${app.push.vapid-subject:mailto:admin@frandm.dev}") String vapidSubject
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.vapidPublicKey = vapidPublicKey != null ? vapidPublicKey.trim() : "";
        this.vapidPrivateKey = vapidPrivateKey != null ? vapidPrivateKey.trim() : "";
        this.vapidSubject = vapidSubject != null ? vapidSubject.trim() : "";
    }

    public VapidKeyResponse getVapidPublicKey() {
        boolean enabled = !vapidPublicKey.isBlank() && !vapidPrivateKey.isBlank();
        return new VapidKeyResponse(vapidPublicKey, enabled);
    }

    @Transactional
    public PushSubscription subscribe(PushSubscriptionRequest request) {
        String p256dh = request.resolveP256dh();
        String auth = request.resolveAuth();

        if (p256dh == null || auth == null) {
            throw new IllegalArgumentException("p256dh and auth keys are required");
        }

        return subscriptionRepository.findByEndpoint(request.endpoint())
                .map(existing -> {
                    existing.setP256dh(p256dh);
                    existing.setAuth(auth);
                    return subscriptionRepository.save(existing);
                })
                .orElseGet(() -> {
                    PushSubscription sub = new PushSubscription();
                    sub.setEndpoint(request.endpoint());
                    sub.setP256dh(p256dh);
                    sub.setAuth(auth);
                    sub.setCreatedAt(OffsetDateTime.now());
                    return subscriptionRepository.save(sub);
                });
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    public void dispatchPush(Notification notification) {
        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            return;
        }

        List<PushSubscription> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            return;
        }

        log.debug("Push notification dispatch ready for {} subscribers (Notification ID: {})",
                subscriptions.size(), notification.getId());
    }
}
