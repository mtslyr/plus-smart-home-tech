package ru.yandex.practicum.telemetry.collector.service.handler.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaEventProducer;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.service.SensorEventHandler;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseSensorEventHandler<T extends SpecificRecordBase> implements SensorEventHandler {
    private final KafkaEventProducer producer;

    protected abstract T mapToAvro(SensorEvent event);

    @Override
    public void handle(SensorEvent event) {
        log.debug("Handle sensor event: {}", event);

        if (!event.getType().equals(getMessageType())) {
            throw new IllegalArgumentException("Неизвестный тип события: " + event.getType());
        }

        T eventAvro = mapToAvro(event);

        ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro avro =
                ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro.newBuilder()
                        .setHubId(event.getHubId())
                        .setId(event.getId())
                        .setTimestamp(event.getTimestamp().toEpochMilli())
                        .setPayload(eventAvro)
                        .build();

        producer.send(producer.sensorEventTopic(), avro, event.getHubId(), event.getTimestamp());
    }
}
