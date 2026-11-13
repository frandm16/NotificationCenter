package com.frandm.notificationcenter.stream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private static final Long EMITTER_TIMEOUT = 30 * 60 * 1000L;

    private final ObjectMapper objectMapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError(e -> {
            emitter.complete();
            emitters.remove(emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                    .comment("ok")
                    .name("connected")
                    .data("Connected to Notification Center stream"));
        } catch (IOException e) {
            removeEmitter(emitter);
        }

        return emitter;
    }

    public void broadcast(String eventName, Object payload) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        String jsonPayload;
        try {
            if (payload instanceof String s) {
                jsonPayload = s;
            } else if (payload instanceof Number n) {
                jsonPayload = n.toString();
            } else {
                jsonPayload = objectMapper.writeValueAsString(payload);
            }
        } catch (Exception e) {
            log.error("Failed to serialize SSE payload for event: {}", eventName, e);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonPayload));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(emitter);
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }

    private void removeEmitter(SseEmitter emitter) {
        if (emitters.remove(emitter)) {
            emitter.complete();
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }
        broadcast("ping", "keep-alive");
    }
}
