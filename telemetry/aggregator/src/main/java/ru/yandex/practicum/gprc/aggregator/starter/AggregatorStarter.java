package ru.yandex.practicum.gprc.aggregator.starter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.gprc.aggregator.configuration.KafkaConfigurationProperties;
import ru.yandex.practicum.gprc.aggregator.exception.RecordProcessException;
import ru.yandex.practicum.gprc.aggregator.service.AggregatorService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorStarter {
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final AggregatorService aggregatorService;
    private final KafkaConfigurationProperties kafkaConfig;
    private final KafkaConsumer<String, SpecificRecordBase> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer;
    private String producerTopic;

    @PostConstruct
    public void postConstruct() {
        this.producerTopic = kafkaConfig.getProducer().getTopic();
        consumer.subscribe(List.of(kafkaConfig.getConsumer().getTopic()));
    }

    public void start() {
        try {
            while(true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(POLL_INTERVAL);

                if (records.isEmpty()) {
                    continue;
                }

                boolean batchHandled = true;
                ConsumerRecord<String, SpecificRecordBase> failed = null;
                for(ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        handleRecord(record);
                    } catch (RecordProcessException e) {
                        log.warn("Exception while process record: {}", e.getMessage());
                        batchHandled = false;
                        failed = e.getFailed();
                    }

                }

                if (batchHandled) {
                    consumer.commitSync();
                    log.info("Offsets commited");
                } else {
                    if (failed != null) {
                        TopicPartition partition = new TopicPartition(failed.topic(), failed.partition());
                        consumer.seek(partition, failed.offset());
                        log.warn("Seek to offset={} partition={}", failed.offset(), failed.partition());

                    }
                }
            }
        } catch (WakeupException ignore) {
            log.info("Catch WakeupException: {}", ignore.getCause().getMessage());
        } finally {
            try {
                consumer.commitSync();
                producer.flush();
            } finally {
                log.info("Closing consumer");
                consumer.close();
                producer.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<String, SpecificRecordBase> record) throws RecordProcessException {
        SensorEventAvro eventAvro = (SensorEventAvro) record.value();
        log.info("Handle event: hubId={} | eventId={}",
                eventAvro.getHubId(),
                eventAvro.getId());

        Optional<SensorsSnapshotAvro> snapshotAvroOpt = aggregatorService.updateState(eventAvro);

        if (snapshotAvroOpt.isPresent()) {
            try {
                sendSnapshot(snapshotAvroOpt.get());
            } catch (RecordProcessException e) {
                e.setFailed(record);
                throw e;
            }
        }
    }

    private void sendSnapshot(SensorsSnapshotAvro snapshot) throws RecordProcessException {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(producerTopic,
                null,
                snapshot.getTimestamp(),
                snapshot.getHubId(),
                snapshot);
        try {
            Future<RecordMetadata> metadataFuture = producer.send(record);
            RecordMetadata metadata = metadataFuture.get();

            log.info("Snapshot sent: topic={} | hubId={} | partition={} | offset={}",
                    metadata.topic(), snapshot.getHubId(), metadata.partition(), metadata.offset());
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            log.warn("Failed to send record: {}", e.getMessage());
            throw new RecordProcessException("Exception while sending record", e);
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}
