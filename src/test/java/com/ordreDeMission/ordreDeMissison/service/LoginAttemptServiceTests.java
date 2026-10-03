package com.ordreDeMission.ordreDeMissison.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTests {

    @Test
    void blocksAfterMaxFailuresWithinWindow() {
        LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(10),
                Duration.ofMinutes(15), 10);
        String key = LoginAttemptService.loginKey("1.2.3.4", "EMP001");

        assertFalse(service.isBlocked(key));
        service.recordFailure(key);
        service.recordFailure(key);
        assertFalse(service.isBlocked(key));
        service.recordFailure(key);
        assertTrue(service.isBlocked(key));
    }

    @Test
    void successClearsFailures() {
        LoginAttemptService service = new LoginAttemptService(2, Duration.ofMinutes(10),
                Duration.ofMinutes(15), 10);
        String key = LoginAttemptService.loginKey("1.2.3.4", "EMP001");

        service.recordFailure(key);
        service.recordSuccess(key);
        service.recordFailure(key);
        assertFalse(service.isBlocked(key));
    }

    @Test
    void windowExpiryResetsCount() throws InterruptedException {
        LoginAttemptService service = new LoginAttemptService(2, Duration.ofMillis(50),
                Duration.ofMinutes(15), 10);
        String key = LoginAttemptService.loginKey("1.2.3.4", "EMP001");

        service.recordFailure(key);
        Thread.sleep(80);
        service.recordFailure(key);
        assertFalse(service.isBlocked(key));
    }

    @Test
    void blockExpiresAfterDuration() throws InterruptedException {
        LoginAttemptService service = new LoginAttemptService(1, Duration.ofMinutes(10),
                Duration.ofMillis(50), 10);
        String key = LoginAttemptService.loginKey("1.2.3.4", "EMP001");

        service.recordFailure(key);
        assertTrue(service.isBlocked(key));
        Thread.sleep(80);
        assertFalse(service.isBlocked(key));
    }

    @Test
    void keysArePerIpAndMatricule() {
        LoginAttemptService service = new LoginAttemptService(1, Duration.ofMinutes(10),
                Duration.ofMinutes(15), 10);

        service.recordFailure(LoginAttemptService.loginKey("1.2.3.4", "EMP001"));

        assertTrue(service.isBlocked(LoginAttemptService.loginKey("1.2.3.4", "EMP001")));
        assertFalse(service.isBlocked(LoginAttemptService.loginKey("1.2.3.4", "EMP002")));
        assertFalse(service.isBlocked(LoginAttemptService.loginKey("5.6.7.8", "EMP001")));
    }

    @Test
    void missionCreationCap() {
        LoginAttemptService service = new LoginAttemptService(5, Duration.ofMinutes(10),
                Duration.ofMinutes(15), 2);
        UUID user = UUID.randomUUID();

        assertTrue(service.isMissionCreationAllowed(user));
        service.recordMissionCreated(user);
        assertTrue(service.isMissionCreationAllowed(user));
        service.recordMissionCreated(user);
        assertFalse(service.isMissionCreationAllowed(user));
    }
}
