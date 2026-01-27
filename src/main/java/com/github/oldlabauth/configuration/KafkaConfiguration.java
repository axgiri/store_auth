package com.github.oldlabauth.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import com.github.oldlabauth.dto.NotificationMessage;

@Configuration
public class KafkaConfiguration {

    @Value("${app.kafka.topic.message}")
    private String messageTopic;

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
    public NewTopic messageTopic() {
        return TopicBuilder.name(messageTopic)
                .partitions(3)
                .replicas(replicas)
                .config("min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }
}
