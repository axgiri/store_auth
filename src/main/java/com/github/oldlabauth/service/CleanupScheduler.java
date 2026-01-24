package com.github.oldlabauth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final RefreshTokenService refreshTokenService;
    private final MessageService messageService;
    private final UserService userService;

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanup() {
        log.info("Starting daily cleanup job");
        
        try {
            refreshTokenService.cleanupExpiredTokens();
            log.debug("Expired refresh tokens cleaned up");
        } catch (Exception e) {
            log.error("Error cleaning up refresh tokens", e);
        }

        try {
            messageService.cleanupOldRecords();
            log.debug("Old message records cleaned up");
        } catch (Exception e) {
            log.error("Error cleaning up message records", e);
        }

        try {
            userService.cleanupUnactivatedUsers();
            log.debug("Unactivated users cleaned up");
        } catch (Exception e) {
            log.error("Error cleaning up unactivated users", e);
        }
        
        log.info("Daily cleanup job finished");
    }
}
