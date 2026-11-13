package com.frandm.notificationcenter.push.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frandm.notificationcenter.notification.model.Notification;
import com.frandm.notificationcenter.push.dto.PushSubscriptionRequest;
import com.frandm.notificationcenter.push.dto.VapidKeyPairResponse;
import com.frandm.notificationcenter.push.dto.VapidKeyResponse;
import com.frandm.notificationcenter.push.model.PushSubscription;
import com.frandm.notificationcenter.push.repository.PushSubscriptionRepository;
import java.security.KeyPair;
import java.security.Security;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Utils;
import nl.martijndwars.webpush.cli.handlers.GenerateKeyHandler;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final String vapidPublicKey;
    private final String vapidPrivateKey;
    private final String vapidSubject;
    private final ObjectMapper objectMapper;
    private PushService pushService;

    public PushNotificationService(
            PushSubscriptionRepository subscriptionRepository,
            @Value("${app.push.vapid-public-key:}") String vapidPublicKey,
            @Value("${app.push.vapid-private-key:}") String vapidPrivateKey,
            @Value("${app.push.vapid-subject:mailto:admin@frandm.dev}") String vapidSubject,
            ObjectMapper objectMapper
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.vapidPublicKey = vapidPublicKey != null ? vapidPublicKey.trim() : "";
        this.vapidPrivateKey = vapidPrivateKey != null ? vapidPrivateKey.trim() : "";
        this.vapidSubject = vapidSubject != null ? vapidSubject.trim() : "";
        this.objectMapper = objectMapper;

        initPushService();
    }

    private void initPushService() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (!vapidPublicKey.isBlank() && !vapidPrivateKey.isBlank()) {
            try {
                PushService service = new PushService();
                service.setPublicKey(Utils.loadPublicKey(vapidPublicKey));
                service.setPrivateKey(Utils.loadPrivateKey(vapidPrivateKey));
                service.setSubject(vapidSubject);
                this.pushService = service;
            } catch (Exception e) {
                log.error("Failed to initialize PushService with configured VAPID keys", e);
                this.pushService = null;
            }
        }
    }

    public VapidKeyPairResponse generateVapidKeyPair() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        try {
            GenerateKeyHandler handler = new GenerateKeyHandler(null);
            KeyPair keyPair = handler.generateKeyPair();

            byte[] pubBytes = Utils.encode((ECPublicKey) keyPair.getPublic());
            String pubBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(pubBytes);

            byte[] privBytes = Utils.encode((ECPrivateKey) keyPair.getPrivate());
            String privBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(privBytes);

            return new VapidKeyPairResponse(pubBase64, privBase64);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate VAPID key pair", e);
        }
    }

    public VapidKeyResponse getVapidPublicKey() {
        boolean enabled = pushService != null && !vapidPublicKey.isBlank();
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

    @Async
    public void dispatchPush(Notification notification) {
        if (pushService == null) {
            return;
        }

        List<PushSubscription> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            return;
        }

        String payloadJson;
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("id", notification.getId());
            map.put("source", notification.getSource());
            map.put("title", notification.getTitle());
            map.put("body", notification.getBody());
            map.put("level", notification.getLevel().name());
            if (notification.getUrl() != null) {
                map.put("url", notification.getUrl());
            }
            if (notification.getDedupKey() != null) {
                map.put("dedupKey", notification.getDedupKey());
            }
            payloadJson = objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize push notification payload for ID: {}", notification.getId(), e);
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                Subscription subscription = new Subscription(
                        sub.getEndpoint(),
                        new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );
                nl.martijndwars.webpush.Notification push = new nl.martijndwars.webpush.Notification(subscription, payloadJson);
                HttpResponse response = pushService.send(push);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 404 || statusCode == 410) {
                    log.info("Push subscription expired for endpoint {}, removing", sub.getEndpoint());
                    subscriptionRepository.delete(sub);
                } else if (statusCode >= 200 && statusCode < 300) {
                    log.debug("Push notification delivered successfully to endpoint {}", sub.getEndpoint());
                } else {
                    log.warn("Push service returned HTTP {} for endpoint {}", statusCode, sub.getEndpoint());
                }
            } catch (Exception e) {
                log.warn("Failed to dispatch push notification to endpoint {}: {}", sub.getEndpoint(), e.getMessage());
            }
        }
    }
}
