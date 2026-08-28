package ru.yandex.practicum.telemetry.collector.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaConfigurationProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer implements AutoCloseable {
    private final KafkaConfigurationProperties config;
    private final KafkaProducer<String, SpecificRecordBase> producer;

    public void send(String topic, SpecificRecordBase event, String hubId, Instant timestamp) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp.toEpochMilli(),
                hubId,
                event
        );

        log.debug("Created producer record: event={}, hubId={}, topic={}",
                event.getClass().getSimpleName(), hubId, topic);

        Future<RecordMetadata> futureMeta = producer.send(record);
        producer.flush();

        try {
            RecordMetadata meta = futureMeta.get();
            log.debug("Record sent: event={}, hubId={}, topic={}, offset={}",
                    event.getClass().getSimpleName(), hubId, meta.topic(), meta.offset());
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Exception while sending record: event={}, hubId={}, topic={}",
                    event.getClass().getSimpleName(), hubId, topic);
            throw new KafkaException(e);
        }
    }

    public String hubEventTopic() {return config.getTopic().getHubEvents();}

    public String sensorEventTopic() {return config.getTopic().getSensorEvents();}

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(5));
    }
}
