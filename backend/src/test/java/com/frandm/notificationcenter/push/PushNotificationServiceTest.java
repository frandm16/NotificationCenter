package com.frandm.notificationcenter.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frandm.notificationcenter.notification.model.Notification;
import com.frandm.notificationcenter.notification.model.NotificationLevel;
import com.frandm.notificationcenter.push.dto.PushSubscriptionRequest;
import com.frandm.notificationcenter.push.dto.VapidKeyPairResponse;
import com.frandm.notificationcenter.push.dto.VapidKeyResponse;
import com.frandm.notificationcenter.push.model.PushSubscription;
import com.frandm.notificationcenter.push.repository.PushSubscriptionRepository;
import com.frandm.notificationcenter.push.service.PushNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class PushNotificationServiceTest {

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldGenerateValidVapidKeyPair() {
        PushNotificationService service = new PushNotificationService(
                subscriptionRepository,
                "",
                "",
                "mailto:admin@frandm.dev",
                objectMapper
        );

        VapidKeyPairResponse keyPair = service.generateVapidKeyPair();
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.publicKey()).isNotBlank();
        assertThat(keyPair.privateKey()).isNotBlank();
    }

    @Test
    void shouldInitializeWithValidVapidKeys() {
        PushNotificationService generator = new PushNotificationService(
                subscriptionRepository,
                "",
                "",
                "mailto:admin@frandm.dev",
                objectMapper
        );
        VapidKeyPairResponse keyPair = generator.generateVapidKeyPair();

        PushNotificationService activeService = new PushNotificationService(
                subscriptionRepository,
                keyPair.publicKey(),
                keyPair.privateKey(),
                "mailto:admin@frandm.dev",
                objectMapper
        );

        VapidKeyResponse keyResponse = activeService.getVapidPublicKey();
        assertThat(keyResponse.enabled()).isTrue();
        assertThat(keyResponse.publicKey()).isEqualTo(keyPair.publicKey());
    }

    @Test
    void shouldSaveAndRemoveSubscription() {
        PushNotificationService service = new PushNotificationService(
                subscriptionRepository,
                "",
                "",
                "mailto:admin@frandm.dev",
                objectMapper
        );

        PushSubscriptionRequest request = new PushSubscriptionRequest(
                "https://updates.push.services.mozilla.com/wpush/v2/test-endpoint",
                new PushSubscriptionRequest.Keys("test-p256dh-key", "test-auth-secret"),
                null,
                null
        );

        PushSubscription saved = service.subscribe(request);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEndpoint()).isEqualTo(request.endpoint());
        assertThat(subscriptionRepository.count()).isEqualTo(1);

        service.unsubscribe(request.endpoint());
        assertThat(subscriptionRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldNotFailWhenDispatchingWithoutSubscribers() {
        PushNotificationService generator = new PushNotificationService(
                subscriptionRepository,
                "",
                "",
                "mailto:admin@frandm.dev",
                objectMapper
        );
        VapidKeyPairResponse keyPair = generator.generateVapidKeyPair();

        PushNotificationService activeService = new PushNotificationService(
                subscriptionRepository,
                keyPair.publicKey(),
                keyPair.privateKey(),
                "mailto:admin@frandm.dev",
                objectMapper
        );

        Notification notification = new Notification();
        notification.setId(100L);
        notification.setSource("studyzen");
        notification.setTitle("Task Due");
        notification.setBody("Physics project due tomorrow");
        notification.setLevel(NotificationLevel.WARNING);

        activeService.dispatchPush(notification);
    }
}
