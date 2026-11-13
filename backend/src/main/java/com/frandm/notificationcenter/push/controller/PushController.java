package com.frandm.notificationcenter.push.controller;

import com.frandm.notificationcenter.push.dto.PushSubscriptionRequest;
import com.frandm.notificationcenter.push.dto.VapidKeyPairResponse;
import com.frandm.notificationcenter.push.dto.VapidKeyResponse;
import com.frandm.notificationcenter.push.model.PushSubscription;
import com.frandm.notificationcenter.push.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push")
public class PushController {

    private final PushNotificationService pushService;

    public PushController(PushNotificationService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/vapid-public-key")
    public VapidKeyResponse getVapidPublicKey() {
        return pushService.getVapidPublicKey();
    }

    @PostMapping("/generate-keys")
    public ResponseEntity<VapidKeyPairResponse> generateKeys() {
        return ResponseEntity.ok(pushService.generateVapidKeyPair());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<PushSubscription> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        PushSubscription subscription = pushService.subscribe(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(@RequestParam("endpoint") String endpoint) {
        pushService.unsubscribe(endpoint);
        return ResponseEntity.noContent().build();
    }
}
