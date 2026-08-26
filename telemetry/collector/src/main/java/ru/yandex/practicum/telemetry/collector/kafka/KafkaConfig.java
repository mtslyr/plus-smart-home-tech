package ru.yandex.practicum.telemetry.collector.kafka;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Component
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapserver;

    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;

    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerializer;

    @Value("${spring.kafka.topic.sensor-events}")
    private String sensorEventTopic;

    @Value("${spring.kafka.topic.hub-events}")
    private String hubEventTopic;

    private Properties producerConfig;

    @PostConstruct
    public void createConfig() {
        this.producerConfig = new Properties();
        this.producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapserver);
        this.producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        this.producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
    }
}
