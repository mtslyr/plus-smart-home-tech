package ru.yandex.practicum.telemetry.collector.service.handler.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaEventProducer;
import ru.yandex.practicum.telemetry.collector.model.EnumMapper;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEventType;
import ru.yandex.practicum.telemetry.collector.model.hub.ScenarioAddedEvent;

@Component(value = "SCENARIO_ADDED")
public class ScenarioAddedEventHandler extends BaseHubEventHandler<ScenarioAddedEventAvro> {

    public ScenarioAddedEventHandler(KafkaEventProducer producer) {
        super(producer);
    }

    @Override
    protected ScenarioAddedEventAvro mapToAvro(HubEvent event) {
        ScenarioAddedEvent _event = (ScenarioAddedEvent) event;
        return ScenarioAddedEventAvro.newBuilder()
                .setName(_event.getName())
                .setActions(_event.getActions().stream()
                        .map(action -> DeviceActionAvro
                                .newBuilder()
                                .setSensorId(action.getSensorId())
                                .setType(EnumMapper.map(action.getType(), ActionTypeAvro.class))
                                .setValue(action.getValue())
                                .build())
                        .toList())
                .setConditions(_event.getConditions().stream()
                        .map(cond -> ScenarioConditionAvro
                                .newBuilder()
                                .setSensorId(cond.getSensorId())
                                .setType(EnumMapper.map(cond.getType(), ConditionTypeAvro.class))
                                .setOperation(EnumMapper.map(cond.getOperation(), ConditionOperationAvro.class))
                                .setValue(cond.getValue())
                                .build())
                        .toList())
                .build();
    }

    @Override
    public HubEventType getMessageType() {
        return HubEventType.SCENARIO_ADDED;
    }
}
