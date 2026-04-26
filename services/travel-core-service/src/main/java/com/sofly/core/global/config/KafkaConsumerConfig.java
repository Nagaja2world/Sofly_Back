package com.sofly.core.global.config;

import com.sofly.core.global.kafka.dto.FlightSavedMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, FlightSavedMessage> flightSavedConsumerFactory() {
        JsonDeserializer<FlightSavedMessage> deserializer = new JsonDeserializer<>(FlightSavedMessage.class);
        deserializer.addTrustedPackages("com.sofly.core.*");
        // 타입 헤더 무시하고 위에서 지정한 FlightSavedMessage로 고정 역직렬화
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FlightSavedMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FlightSavedMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(flightSavedConsumerFactory());
        return factory;
    }
}
