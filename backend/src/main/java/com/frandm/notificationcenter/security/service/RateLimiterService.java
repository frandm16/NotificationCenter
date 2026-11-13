package com.frandm.notificationcenter.security.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_SECONDS = 15 * 60L;

    private static record AttemptRecord(int count, Instant lockedUntil) {}

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptRecord record = attempts.get(key);
        if (record == null) {
            return false;
        }
        if (record.lockedUntil() != null) {
            if (Instant.now().isBefore(record.lockedUntil())) {
                return true;
            }
            attempts.remove(key);
            return false;
        }
        return false;
    }

    public long getRemainingLockoutSeconds(String key) {
        AttemptRecord record = attempts.get(key);
        if (record != null && record.lockedUntil() != null) {
            long remaining = record.lockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(0, remaining);
        }
        return 0;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, existing) -> {
            int newCount = (existing == null) ? 1 : existing.count() + 1;
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                return new AttemptRecord(newCount, Instant.now().plusSeconds(LOCKOUT_DURATION_SECONDS));
            }
            return new AttemptRecord(newCount, null);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }
}
