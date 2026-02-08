package com.github.storeauth.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.github.storeauth.dto.MessageChannelEnum;
import com.github.storeauth.dto.NotificationMessage;
import com.github.storeauth.dto.RegistrationCompensateMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    @Value("${app.kafka.topic.message}")
    private String messageTopic;

    @Value("${app.kafka.topic.registration-compensate}")
    private String registrationCompensateTopic;

    private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;
    private final KafkaTemplate<String, RegistrationCompensateMessage> registrationCompensateKafkaTemplate;

    public void sendOtp(MessageChannelEnum channel, String destination, int otp) {
        switch (channel) {
            case SMS -> sendSmsOtp(destination, otp);
            case EMAIL -> sendEmailOtp(destination, otp);
        }
    }

    // public void sendNotification(MessageChannelEnum channel, String destination,
    // String message, boolean isHtml) {
    // switch (channel) {
    // case SMS -> sendSmsNotification(destination, message);
    // case EMAIL -> sendEmailNotification(destination, message, isHtml);
    // }
    // }

    // private void sendSmsNotification(String phoneNumber, String text) {
    //     log.debug("sending notification via SMS to phone number: {}", phoneNumber);
    //     var message = new NotificationMessage(phoneNumber, text, false, "Notification");
    //     kafkaTemplate.send(messageTopic, "message-sms", message);
    // }

    // private void sendEmailNotification(String email, String text, boolean isHtml) {
    //     log.debug("sending notification via Email to: {}", email);
    //     var message = new NotificationMessage(email, text, isHtml, "Notification");
    //     kafkaTemplate.send(messageTopic, "message-email", message);
    // }

    private void sendSmsOtp(String phoneNumber, int otp) {
        log.debug("sending OTP via SMS to phone number: {}", phoneNumber);
        var message = new NotificationMessage(phoneNumber, String.valueOf(otp), false, "OTP");
        kafkaTemplate.send(messageTopic, "message-sms", message);
    }

    private void sendEmailOtp(String email, int otp) {
        log.debug("sending OTP via Email to: {}", email);
        var message = new NotificationMessage(email, String.valueOf(otp), false, "OTP");
        kafkaTemplate.send(messageTopic, "message-email", message);
    }

    public void compensateRegistration(UUID idempotencyKey) {
        log.debug("Compensating registration for idempotency key: {}", idempotencyKey);
        registrationCompensateKafkaTemplate.send(registrationCompensateTopic,
                idempotencyKey.toString(),
                new RegistrationCompensateMessage(idempotencyKey));
    }
}
