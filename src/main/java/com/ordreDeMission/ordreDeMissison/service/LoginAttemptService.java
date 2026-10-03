package com.ordreDeMission.ordreDeMissison.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiting. Single-instance app: counters reset on restart,
 * which is acceptable and documented. All updates go through
 * ConcurrentHashMap.compute for atomicity.
 */
@Service
public class LoginAttemptService {

    private final int maxFailures;
    private final Duration failWindow;
    private final Duration blockDuration;
    private final int maxCreationsPerHour;

    private record LoginAttempt(int failures, LocalDateTime windowStart, LocalDateTime blockedUntil) {}
    private record CreationCount(int count, LocalDateTime windowStart) {}

    private final ConcurrentHashMap<String, LoginAttempt> logins = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CreationCount> creations = new ConcurrentHashMap<>();

    public LoginAttemptService() {
        this(5, Duration.ofMinutes(10), Duration.ofMinutes(15), 10);
    }

    LoginAttemptService(int maxFailures, Duration failWindow, Duration blockDuration, int maxCreationsPerHour) {
        this.maxFailures = maxFailures;
        this.failWindow = failWindow;
        this.blockDuration = blockDuration;
        this.maxCreationsPerHour = maxCreationsPerHour;
    }

    public static String loginKey(String ip, String matricule) {
        String m = matricule == null ? "unknown" : matricule.trim().toLowerCase();
        return "login:" + ip + ":" + m;
    }

    /**
     * Client IP, proxy-aware: X-Forwarded-For is only trusted when the
     * connection itself comes from localhost (our Nginx setup).
     */
    public static String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if ("127.0.0.1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remote;
    }

    public boolean isBlocked(String key) {
        LoginAttempt attempt = logins.get(key);
        if (attempt == null || attempt.blockedUntil() == null) return false;
        if (attempt.blockedUntil().isAfter(LocalDateTime.now())) return true;
        logins.remove(key);
        return false;
    }

    public void recordFailure(String key) {
        logins.compute(key, (k, prev) -> {
            LocalDateTime now = LocalDateTime.now();
            if (prev == null || prev.windowStart().plus(failWindow).isBefore(now)) {
                prev = new LoginAttempt(0, now, null);
            }
            int failures = prev.failures() + 1;
            LocalDateTime blockedUntil = failures >= maxFailures ? now.plus(blockDuration) : null;
            return new LoginAttempt(failures, prev.windowStart(), blockedUntil);
        });
    }

    public void recordSuccess(String key) {
        logins.remove(key);
    }

    public boolean isMissionCreationAllowed(UUID userId) {
        CreationCount count = creations.get(creationKey(userId));
        if (count == null) return true;
        if (count.windowStart().plusHours(1).isBefore(LocalDateTime.now())) {
            creations.remove(creationKey(userId));
            return true;
        }
        return count.count() < maxCreationsPerHour;
    }

    public void recordMissionCreated(UUID userId) {
        creations.compute(creationKey(userId), (k, prev) -> {
            LocalDateTime now = LocalDateTime.now();
            if (prev == null || prev.windowStart().plusHours(1).isBefore(now)) {
                return new CreationCount(1, now);
            }
            return new CreationCount(prev.count() + 1, prev.windowStart());
        });
    }

    private static String creationKey(UUID userId) {
        return "create:" + userId;
    }
}
