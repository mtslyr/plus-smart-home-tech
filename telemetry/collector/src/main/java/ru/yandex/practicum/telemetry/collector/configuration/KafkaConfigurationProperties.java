package ru.yandex.practicum.telemetry.collector.configuration;

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
    Topic topic;
    Producer producer;

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Topic {
        String hubEvents;
        String sensorEvents;
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Producer {
        String keySerializer;
        String valueSerializer;
    }
}
