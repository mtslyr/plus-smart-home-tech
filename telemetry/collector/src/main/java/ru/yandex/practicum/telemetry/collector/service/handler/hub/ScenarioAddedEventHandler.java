package ru.yandex.practicum.telemetry.collector.service.handler.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaEventProducer;
import util.ActionTypeMapper;
import util.ConditionTypeMapper;
import util.OperationTypeMapper;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto.ValueCase.BOOL_VALUE;

@Component(value = "SCENARIO_ADDED")
public class ScenarioAddedEventHandler extends BaseHubEventHandler<ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro> {

    public ScenarioAddedEventHandler(KafkaEventProducer producer) {
        super(producer);
    }

    @Override
    protected ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro mapToAvro(HubEventProto event) {
        ScenarioAddedEventProto _event = event.getScenarioAdded();

        List<ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro> actions = new ArrayList<>();
        List<ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro> conditions = new ArrayList<>();

        _event.getActionList().forEach(actionProto -> {
            ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro actionTypeAvro = ActionTypeMapper.convert(actionProto.getType());
            ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro deviceActionAvro = ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro.newBuilder()
                    .setSensorId(actionProto.getSensorId())
                    .setType(actionTypeAvro)
                    .setValue(actionProto.getValue())
                    .build();

            actions.add(deviceActionAvro);
        });

        _event.getConditionList().forEach(scenarioConditionProto -> {
            ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro conditionType = ConditionTypeMapper.convert(scenarioConditionProto.getType());
            ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro operationType = OperationTypeMapper.convert(scenarioConditionProto.getOperation());

            ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro scenarioConditionAvro = ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro.newBuilder()
                    .setSensorId(scenarioConditionProto.getSensorId())
                    .setType(conditionType)
                    .setOperation(operationType)
                    .setValue(
                            scenarioConditionProto.getValueCase().equals(BOOL_VALUE) ?
                                    scenarioConditionProto.getBoolValue()
                                    : scenarioConditionProto.getIntValue())
                    .build();

            conditions.add(scenarioConditionAvro);
        });


        return ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro.newBuilder()
                .setName(_event.getName())
                .setActions(actions)
                .setConditions(conditions)
                .build();
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }
}
