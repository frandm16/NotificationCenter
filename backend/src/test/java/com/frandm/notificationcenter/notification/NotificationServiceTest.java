package com.frandm.notificationcenter.notification;

import com.frandm.notificationcenter.ingest.dto.IngestNotificationRequest;
import com.frandm.notificationcenter.notification.dto.NotificationResponse;
import com.frandm.notificationcenter.notification.model.NotificationLevel;
import com.frandm.notificationcenter.notification.repository.NotificationRepository;
import com.frandm.notificationcenter.notification.service.NotificationService;
import com.frandm.notificationcenter.push.repository.PushSubscriptionRepository;
import com.frandm.notificationcenter.push.service.PushNotificationService;
import com.frandm.notificationcenter.stream.service.SseEmitterService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class NotificationServiceTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        SseEmitterService sseEmitterService = new SseEmitterService();
        PushNotificationService pushService = new PushNotificationService(pushSubscriptionRepository, "", "", "");
        notificationService = new NotificationService(notificationRepository, sseEmitterService, pushService);
    }

    @Test
    void shouldIngestNotificationAndEmitEvents() {
        IngestNotificationRequest request = new IngestNotificationRequest(
                "Backup Completed",
                "Automated backup finished with status SUCCESS",
                NotificationLevel.SUCCESS,
                "https://notifications.frandm.dev",
                null
        );

        NotificationResponse response = notificationService.ingest("justenoughbackups", request);

        assertThat(response.id()).isNotNull();
        assertThat(response.source()).isEqualTo("justenoughbackups");
        assertThat(response.title()).isEqualTo("Backup Completed");
        assertThat(response.level()).isEqualTo(NotificationLevel.SUCCESS);
        assertThat(response.read()).isFalse();
    }

    @Test
    void shouldDeduplicateWhenDedupKeyMatchesUnreadNotification() {
        IngestNotificationRequest first = new IngestNotificationRequest(
                "UPS on Battery",
                "Runtime remaining: 45 min",
                NotificationLevel.WARNING,
                null,
                "ups-battery-alert"
        );
        NotificationResponse firstResp = notificationService.ingest("nut", first);

        IngestNotificationRequest second = new IngestNotificationRequest(
                "UPS on Battery",
                "Runtime remaining: 30 min",
                NotificationLevel.WARNING,
                null,
                "ups-battery-alert"
        );
        NotificationResponse secondResp = notificationService.ingest("nut", second);

        assertThat(secondResp.id()).isEqualTo(firstResp.id());
        assertThat(secondResp.body()).isEqualTo("Runtime remaining: 30 min");
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldMarkAsReadAndMarkAllRead() {
        notificationService.ingest("studyzen", new IngestNotificationRequest("Exam Alert", "Math tomorrow", NotificationLevel.INFO, null, null));
        notificationService.ingest("studyzen", new IngestNotificationRequest("Deadline Alert", "Physics essay", NotificationLevel.WARNING, null, null));

        assertThat(notificationService.getUnreadCount()).isEqualTo(2);

        Page<NotificationResponse> page = notificationService.getNotifications(null, null, null, PageRequest.of(0, 10));
        Long firstId = page.getContent().get(0).id();

        Optional<NotificationResponse> readOpt = notificationService.markRead(firstId);
        assertThat(readOpt).isPresent();
        assertThat(readOpt.get().read()).isTrue();
        assertThat(notificationService.getUnreadCount()).isEqualTo(1);

        notificationService.markAllRead();
        assertThat(notificationService.getUnreadCount()).isEqualTo(0);
    }
}
