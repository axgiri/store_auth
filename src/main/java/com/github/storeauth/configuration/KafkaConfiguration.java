package com.github.storeauth.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.github.storeauth.dto.NotificationMessage;
import com.github.storeauth.dto.RegistrationCompensateMessage;
import com.github.storeauth.dto.request.UserCreateRequest;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class KafkaConfiguration {

    @Value("${app.kafka.topic.message}")
    private String messageTopic;

    @Value("${app.kafka.topic.registration}")
    private String registrationTopic;

    @Value("${app.kafka.topic.registration-compensate}")
    private String registrationCompensateTopic;

    @Value("${app.kafka.replicas}")
    private int replicas;

    @Value("${app.kafka.min-insync-replicas}")
    private int minInSyncReplicas;

    @Bean
    public ProducerFactory<String, NotificationMessage> notificationProducerFactory(KafkaProperties kafkaProperties, JsonMapper jsonMapper) {
        var props = kafkaProperties.buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JacksonJsonSerializer<>(jsonMapper));
    }

    @Bean
    public KafkaTemplate<String, NotificationMessage> notificationKafkaTemplate(ProducerFactory<String, NotificationMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, UserCreateRequest> registrationConsumerFactory(KafkaProperties kafkaProperties, JsonMapper jsonMapper) {
        var props = kafkaProperties.buildConsumerProperties();
        var deserializer = new JacksonJsonDeserializer<>(UserCreateRequest.class, jsonMapper);
        deserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserCreateRequest> registrationKafkaListenerContainerFactory(
            ConsumerFactory<String, UserCreateRequest> registrationConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, UserCreateRequest>();
        factory.setConsumerFactory(registrationConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 3)));
        return factory;
    }

    @Bean
    public ProducerFactory<String, RegistrationCompensateMessage> registrationCompensateProducerFactory(KafkaProperties kafkaProperties, JsonMapper jsonMapper) {
        var props = kafkaProperties.buildProducerProperties();
        var serializer = new JacksonJsonSerializer<RegistrationCompensateMessage>(jsonMapper);
        serializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), serializer);
    }

    @Bean
    public KafkaTemplate<String, RegistrationCompensateMessage> registrationCompensateKafkaTemplate(ProducerFactory<String, RegistrationCompensateMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic messageTopic() {
        return TopicBuilder.name(messageTopic)
                .partitions(3)
                .replicas(replicas)
                .config("min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }

    @Bean
    public NewTopic registrationCompensateTopic() {
        return TopicBuilder.name(registrationCompensateTopic)
                .partitions(3)
                .replicas(replicas)
                .config("min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }
}