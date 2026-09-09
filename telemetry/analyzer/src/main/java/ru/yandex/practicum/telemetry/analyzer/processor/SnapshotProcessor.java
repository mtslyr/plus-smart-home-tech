package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.analyzer.configuration.KafkaConfigurationProperties;
import ru.yandex.practicum.telemetry.analyzer.service.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final SnapshotService snapshotService;
    private final KafkaConsumer<String, SpecificRecordBase> snapshotsConsumer;
    private final KafkaConfigurationProperties kafkaConfig;

    @Override
    public void run() {
        String topic = kafkaConfig.getConsumer().getSnapshots().getTopic();
        snapshotsConsumer.subscribe(List.of(topic));

        log.info("SnapshotProcessor started, subscribed to topic: {}", topic);

        try {
            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = snapshotsConsumer.poll(POLL_INTERVAL);

                if (records.isEmpty()) {
                    continue;
                }

                List<ConsumerRecord<String, SpecificRecordBase>> failedRecords = new ArrayList<>();
                List<SensorsSnapshotAvro> validSnapshots = new ArrayList<>();

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        SensorsSnapshotAvro snapshot = deserializeSnapshot(record);
                        if (snapshot != null) {
                            validSnapshots.add(snapshot);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to deserialize record at offset {}: {}", record.offset(), e.getMessage());
                        failedRecords.add(record);
                    }
                }

                if (!validSnapshots.isEmpty()) {
                    snapshotService.processBatch(validSnapshots);
                }

                if (!failedRecords.isEmpty()) {
                    for (ConsumerRecord<String, SpecificRecordBase> failed : failedRecords) {
                        TopicPartition partition = new TopicPartition(failed.topic(), failed.partition());
                        snapshotsConsumer.seek(partition, failed.offset());
                    }
                    log.warn("Seek failed records, total: {}", failedRecords.size());
                }

                snapshotsConsumer.commitSync();
            }
        } catch (WakeupException ignore) {
        } finally {
            try {
                snapshotsConsumer.commitSync();
            } finally {
                snapshotsConsumer.close();
            }
        }
    }

    private SensorsSnapshotAvro deserializeSnapshot(ConsumerRecord<String, SpecificRecordBase> record) {
        if (record.value() == null) {
            return null;
        }
        return (SensorsSnapshotAvro) record.value();
    }

    public void shutdown() {
        snapshotsConsumer.wakeup();
    }
}
