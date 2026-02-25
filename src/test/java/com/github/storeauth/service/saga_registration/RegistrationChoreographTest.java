package com.github.storeauth.service.saga_registration;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.storeauth.dto.request.UserCreateRequest;
import com.github.storeauth.entity.Role;
import com.github.storeauth.exception.UserAlreadyExistsException;
import com.github.storeauth.service.EventService;
import com.github.storeauth.service.RegistrationService;

@ExtendWith(MockitoExtension.class)
class RegistrationChoreographTest {

    @Mock private RegistrationService registrationService;
    @Mock private EventService eventService;

    @InjectMocks
    private RegistrationChoreograph choreograph;

    private UserCreateRequest request(String email) {
        return new UserCreateRequest(UUID.randomUUID(), email, "password", Role.USER);
    }

    @Test
    @DisplayName("creates user when email is valid and unique")
    void createsUser_whenValid() {
        var req = request("new@test.com");
        when(registrationService.validateEmail(req.email())).thenReturn(true);

        choreograph.createUser(req);

        verify(registrationService).create(req);
    }

    @Test
    @DisplayName("skips creation when email already used")
    void skips_whenEmailInvalid() {
        var req = request("taken@test.com");
        when(registrationService.validateEmail(req.email())).thenReturn(false);

        choreograph.createUser(req);

        verify(registrationService, never()).create(req);
    }

    @Test
    @DisplayName("does not compensate when UserAlreadyExistsException thrown")
    void noCompensation_whenAlreadyExists() {
        var req = request("dup@test.com");
        when(registrationService.validateEmail(req.email())).thenReturn(true);
        doThrow(new UserAlreadyExistsException("exists")).when(registrationService).create(req);

        choreograph.createUser(req);

        verify(eventService, never()).compensateRegistration(req.idempotencyKey());
    }

    @Test
    @DisplayName("compensates registration on unexpected exception")
    void compensates_onUnexpectedError() {
        var req = request("err@test.com");
        when(registrationService.validateEmail(req.email())).thenReturn(true);
        doThrow(new RuntimeException("db error")).when(registrationService).create(req);

        choreograph.createUser(req);

        verify(eventService).compensateRegistration(req.idempotencyKey());
    }
}
