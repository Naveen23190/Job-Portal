package com.jobportal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(LiveUpdateService.class);
    private static final long CONNECTION_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MILLIS);
        Set<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(exception -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ready"));
        } catch (IOException | IllegalStateException exception) {
            remove(userId, emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void publishApplicationUpdate(Long userId) {
        publish(userId, "application-updated");
    }

    public void publishInterviewUpdate(Long userId) {
        publish(userId, "interview-updated");
    }

    @Scheduled(fixedRate = 20000)
    public void sendHeartbeats() {
        emitters.forEach((userId, userEmitters) -> userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException | IllegalStateException exception) {
                logger.debug("Removing disconnected live-update subscriber for user {}", userId);
                remove(userId, emitter);
                emitter.completeWithError(exception);
            }
        }));
    }

    private void publish(Long userId, String eventName) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;

        userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data("updated"));
            } catch (IOException | IllegalStateException exception) {
                logger.debug("Removing disconnected live-update subscriber for user {}", userId);
                remove(userId, emitter);
                emitter.completeWithError(exception);
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) emitters.remove(userId, userEmitters);
    }
}
