package ru.yandex.practicum.gprc.aggregator.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaConfigurationProperties {
    String bootstrapServers;
    Consumer consumer;
    Producer producer;

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Consumer {
        String groupId;
        String clientId;
        Boolean enableAutoCommit;
        String topic;
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Producer {
        String topic;
    }
}
