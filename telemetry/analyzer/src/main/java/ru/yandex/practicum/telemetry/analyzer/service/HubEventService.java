package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final SensorRepository sensorRepository;

    @Transactional
    public void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        String sensorId = event.getId();
        DeviceTypeAvro deviceType = event.getDeviceType();

        boolean exists = sensorRepository.findByIdAndHubId(sensorId, hubId).isPresent();
        if (exists) {
            log.debug("Sensor {} already exists for hub {}", sensorId, hubId);
            return;
        }

        Sensor sensor = Sensor.builder()
                .id(sensorId)
                .hubId(hubId)
                .build();
        sensorRepository.save(sensor);
        log.info("Device added: sensorId={}, hubId={}, type={}", sensorId, hubId, deviceType);
    }

    @Transactional
    public void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        String sensorId = event.getId();

        Optional<Sensor> sensorOpt = sensorRepository.findByIdAndHubId(sensorId, hubId);
        if (sensorOpt.isEmpty()) {
            log.debug("Sensor {} not found for hub {}", sensorId, hubId);
            return;
        }

        sensorRepository.delete(sensorOpt.get());
        log.info("Device removed: sensorId={}, hubId={}", sensorId, hubId);
    }

    @Transactional
    public void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        String scenarioName = event.getName();

        Optional<Scenario> existingOpt = scenarioRepository.findByHubIdAndName(hubId, scenarioName);
        if (existingOpt.isPresent()) {
            log.debug("Scenario {} already exists for hub {}", scenarioName, hubId);
            return;
        }

        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(scenarioName)
                .build();
        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            saveCondition(scenario.getId(), conditionAvro);
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            saveAction(scenario.getId(), actionAvro);
        }

        log.info("Scenario added: name={}, hubId={}, conditions={}, actions={}",
                scenarioName, hubId, event.getConditions().size(), event.getActions().size());
    }

    @Transactional
    public void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String scenarioName = event.getName();

        Optional<Scenario> scenarioOpt = scenarioRepository.findByHubIdAndName(hubId, scenarioName);
        if (scenarioOpt.isEmpty()) {
            log.debug("Scenario {} not found for hub {}", scenarioName, hubId);
            return;
        }

        Scenario scenario = scenarioOpt.get();
        scenarioConditionRepository.deleteAll(scenarioConditionRepository.findByScenarioId(scenario.getId()));
        scenarioActionRepository.deleteAll(scenarioActionRepository.findByScenarioId(scenario.getId()));
        scenarioRepository.delete(scenario);

        log.info("Scenario removed: name={}, hubId={}", scenarioName, hubId);
    }

    private void saveCondition(Long scenarioId, ScenarioConditionAvro conditionAvro) {
        ConditionTypeAvro type = conditionAvro.getType();
        ConditionOperationAvro operation = conditionAvro.getOperation();

        Condition condition = Condition.builder()
                .type(type.name())
                .operation(operation.name())
                .value(convertValue(conditionAvro.getValue()))
                .build();
        condition = conditionRepository.save(condition);

        ScenarioCondition scenarioCondition = ScenarioCondition.builder()
                .scenarioId(scenarioId)
                .sensorId(conditionAvro.getSensorId())
                .conditionId(condition.getId())
                .build();
        scenarioConditionRepository.save(scenarioCondition);
    }

    private void saveAction(Long scenarioId, DeviceActionAvro actionAvro) {
        Action action = Action.builder()
                .type(actionAvro.getType().name())
                .value(actionAvro.getValue())
                .build();
        action = actionRepository.save(action);

        ScenarioAction scenarioAction = ScenarioAction.builder()
                .scenarioId(scenarioId)
                .sensorId(actionAvro.getSensorId())
                .actionId(action.getId())
                .build();
        scenarioActionRepository.save(scenarioAction);
    }

    private Integer convertValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return null;
    }
}
