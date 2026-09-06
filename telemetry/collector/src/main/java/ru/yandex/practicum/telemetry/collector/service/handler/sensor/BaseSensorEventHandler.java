package ru.yandex.practicum.telemetry.collector.service.handler.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaEventProducer;
import ru.yandex.practicum.telemetry.collector.service.SensorEventHandler;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseSensorEventHandler<T extends SpecificRecordBase> implements SensorEventHandler {
    private final KafkaEventProducer producer;

    protected abstract T mapToAvro(SensorEventProto event);

    @Override
    public void handle(SensorEventProto event) {
        log.debug("Handle sensor event: {}", event);
        SensorEventProto.PayloadCase type = event.getPayloadCase();

        if (!type.equals(getMessageType())) {
            throw new IllegalArgumentException("Неизвестный тип события: " + event.getPayloadCase());
        }

        T eventAvro = mapToAvro(event);

        long timestampMillis = Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ).toEpochMilli();

        ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro avro =
                ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro.newBuilder()
                        .setHubId(event.getHubId())
                        .setId(event.getId())
                        .setTimestamp(timestampMillis)
                        .setPayload(eventAvro)
                        .build();

        producer.send(
                producer.sensorEventTopic(),
                avro,
                event.getHubId(),
                Instant.ofEpochMilli(timestampMillis));
    }
}
