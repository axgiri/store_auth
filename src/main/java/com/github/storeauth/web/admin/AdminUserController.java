package com.github.storeauth.web.admin;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.storeauth.service.RegistrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/sa/users")
public class AdminUserController {
    
    private final RegistrationService registrationService;
    
    @PreAuthorize("@accessControlService.isModerator()")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable String userId) {
        log.debug("Delete attempt for userId: {}", userId);
        registrationService.delete(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
