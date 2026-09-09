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
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    private final HubEventService hubEventService;
    private final KafkaConsumer<String, SpecificRecordBase> hubEventsConsumer;
    private final KafkaConfigurationProperties kafkaConfig;

    @Override
    public void run() {
        String topic = kafkaConfig.getConsumer().getHubEvents().getTopic();
        hubEventsConsumer.subscribe(List.of(topic));

        log.info("HubEventProcessor started, subscribed to topic: {}", topic);

        try {
            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = hubEventsConsumer.poll(POLL_INTERVAL);

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        processRecord(record);
                    } catch (Exception e) {
                        log.warn("Failed to process hub event at offset {}: {}", record.offset(), e.getMessage());
                        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
                        hubEventsConsumer.seek(partition, record.offset());
                    }
                }

                hubEventsConsumer.commitSync();
            }
        } catch (WakeupException ignore) {
        } finally {
            try {
                hubEventsConsumer.commitSync();
            } finally {
                hubEventsConsumer.close();
            }
        }
    }

    private void processRecord(ConsumerRecord<String, SpecificRecordBase> record) {
        HubEventAvro event = (HubEventAvro) record.value();
        String hubId = event.getHubId();

        if (event.getPayload() instanceof DeviceAddedEventAvro) {
            hubEventService.handleDeviceAdded(hubId, (DeviceAddedEventAvro) event.getPayload());
        } else if (event.getPayload() instanceof DeviceRemovedEventAvro) {
            hubEventService.handleDeviceRemoved(hubId, (DeviceRemovedEventAvro) event.getPayload());
        } else if (event.getPayload() instanceof ScenarioAddedEventAvro) {
            hubEventService.handleScenarioAdded(hubId, (ScenarioAddedEventAvro) event.getPayload());
        } else if (event.getPayload() instanceof ScenarioRemovedEventAvro) {
            hubEventService.handleScenarioRemoved(hubId, (ScenarioRemovedEventAvro) event.getPayload());
        }
    }

    public void shutdown() {
        hubEventsConsumer.wakeup();
    }
}
