package com.github.storeauth.service.saga_registration;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import com.github.storeauth.dto.request.UserCreateRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationConsumer {

    private final RegistrationChoreograph registrationChoreograph;

    @KafkaListener(
        topics = "${app.kafka.topic.registration}",
        containerFactory = "registrationKafkaListenerContainerFactory",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleRegistration(
            @Payload UserCreateRequest request,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received registration event: key={}, partition={}, offset={}, email={}", key, partition, offset, request.email());
            registrationChoreograph.createUser(request);
        log.info("Successfully registered user with email: {}", request.email());
    }
}

