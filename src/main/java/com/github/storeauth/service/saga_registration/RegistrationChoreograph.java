package com.github.storeauth.service.saga_registration;

import org.springframework.stereotype.Service;

import com.github.storeauth.dto.request.UserCreateRequest;
import com.github.storeauth.exception.UserAlreadyExistsException;
import com.github.storeauth.service.EventService;
import com.github.storeauth.service.RegistrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationChoreograph {
    
    private final RegistrationService registrationService;
    private final EventService eventService;

    public void createUser(UserCreateRequest request){
        if(!registrationService.validateEmail(request.email())) {
            return;
        }

        try{
            registrationService.create(request);
        } catch(UserAlreadyExistsException e) {
            log.info("User with email {} already exists, skipping registration", request.email());
            return;
        }
        catch (Exception e) {
            log.info("Error during user registration, initiating compensation for email: {}", request.email());
            eventService.compensateRegistration(request.idempotencyKey());
        }
    }
}
