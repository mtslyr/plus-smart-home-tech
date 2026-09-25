package ru.yandex.practicum.telemetry.analyzer.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaConfigurationProperties {
    String bootstrapServers;
    Consumer consumer;

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Consumer {
        Boolean enableAutoCommit;
        ConsumerConfig snapshots;
        ConsumerConfig hubEvents;
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ConsumerConfig {
        String groupId;
        String clientId;
        String topic;
    }
}
