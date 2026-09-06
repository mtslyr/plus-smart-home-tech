package ru.yandex.practicum.telemetry.collector.service.handler.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaEventProducer;
import ru.yandex.practicum.telemetry.collector.service.HubEventHandler;
import util.DeviceTypeMapper;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseHubEventHandler<T extends SpecificRecordBase> implements HubEventHandler {

    private final KafkaEventProducer producer;

    protected abstract T mapToAvro(HubEventProto event);

    @Override
    public void handle(HubEventProto event) {
        log.debug("Handle hub event: {}", event);
        HubEventProto.PayloadCase type =  event.getPayloadCase();

        if (!type.equals(getMessageType())) {
            throw new IllegalArgumentException("Неизвестный тип события: " + event.getPayloadCase());
        }

        T eventAvro = mapToAvro(event);

        ru.yandex.practicum.kafka.telemetry.event.HubEventAvro avro =
                ru.yandex.practicum.kafka.telemetry.event.HubEventAvro.newBuilder()
                        .setHubId(event.getHubId())
                        .setTimestamp(event.getTimestamp().getSeconds())
                        .setPayload(eventAvro)
                        .build();

        producer.send(
                producer.hubEventTopic(),
                avro,
                event.getHubId(),
                Instant.ofEpochSecond(event.getTimestamp().getSeconds()));
    }
}
