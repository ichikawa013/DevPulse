package com.devpulse.notification_service.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public Map<String, Object> baseConfig(){
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.devpulse.notification_service.event");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return config;
    }

    @Bean
    public ConsumerFactory<String, Object> postEventConsumerFactory() {
        Map<String, Object> postConsumerConfig = baseConfig();
        postConsumerConfig.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.devpulse.notification_service.dto.events.PostEvent");
        return new DefaultKafkaConsumerFactory<>(postConsumerConfig);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> postEventListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(postEventConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> reactionEventConsumerFactory() {
        Map<String, Object> reactionConsumerConfig = baseConfig();
        reactionConsumerConfig.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.devpulse.notification_service.dto.events.ReactionEvent");
        return new DefaultKafkaConsumerFactory<>(reactionConsumerConfig);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> reactionEventListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reactionEventConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}
