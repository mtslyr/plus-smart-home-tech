package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final ScenarioService scenarioService;
    private final java.util.concurrent.ExecutorService executorService;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void processBatch(List<SensorsSnapshotAvro> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }

        Map<String, List<SensorsSnapshotAvro>> snapshotsByHub = snapshots.stream()
                .collect(Collectors.groupingBy(SensorsSnapshotAvro::getHubId));

        Set<String> hubIds = snapshotsByHub.keySet();
        Map<String, List<Scenario>> scenariosByHub = new HashMap<>();
        for (String hubId : hubIds) {
            List<Scenario> scenarios = scenarioService.findByHubId(hubId);
            if (!scenarios.isEmpty()) {
                scenariosByHub.put(hubId, scenarios);
            }
        }

        Set<Long> allScenarioIds = scenariosByHub.values().stream()
                .flatMap(List::stream)
                .map(Scenario::getId)
                .collect(Collectors.toSet());

        Map<Long, List<ScenarioCondition>> conditionsByScenario = scenarioService.findConditionsByScenarioIds(allScenarioIds);
        Map<Long, List<ScenarioAction>> actionsByScenario = scenarioService.findActionsByScenarioIds(allScenarioIds);

        Set<Long> conditionIds = conditionsByScenario.values().stream()
                .flatMap(List::stream)
                .map(c -> c.getConditionId())
                .collect(Collectors.toSet());

        Set<Long> actionIds = actionsByScenario.values().stream()
                .flatMap(List::stream)
                .map(a -> a.getActionId())
                .collect(Collectors.toSet());

        Map<Long, Condition> conditionsById = scenarioService.findConditionsByIds(conditionIds);
        Map<Long, Action> actionsById = scenarioService.findActionsByIds(actionIds);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<String, List<SensorsSnapshotAvro>> entry : snapshotsByHub.entrySet()) {
            String hubId = entry.getKey();
            List<Scenario> scenarios = scenariosByHub.get(hubId);

            if (scenarios == null || scenarios.isEmpty()) {
                continue;
            }

            for (SensorsSnapshotAvro snapshot : entry.getValue()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        processSnapshotForHub(snapshot, scenarios, conditionsByScenario, actionsByScenario,
                                conditionsById, actionsById);
                    } catch (Exception e) {
                        log.error("Error processing snapshot for hub {}: {}", hubId, e.getMessage(), e);
                    }
                }, executorService);
                futures.add(future);
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processSnapshotForHub(SensorsSnapshotAvro snapshot, List<Scenario> scenarios,
                                       Map<Long, List<ScenarioCondition>> conditionsByScenario,
                                       Map<Long, List<ScenarioAction>> actionsByScenario,
                                       Map<Long, Condition> conditionsById,
                                       Map<Long, Action> actionsById) {

        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        Timestamp timestamp = toTimestampProto(snapshot.getTimestamp());

        for (Scenario scenario : scenarios) {
            List<ScenarioCondition> conditions = conditionsByScenario.getOrDefault(scenario.getId(), List.of());
            List<ScenarioAction> actions = actionsByScenario.getOrDefault(scenario.getId(), List.of());

            if (conditions.isEmpty()) {
                continue;
            }

            boolean allConditionsMet = checkConditions(conditions, sensorsState, conditionsById);

            if (allConditionsMet) {
                executeActions(snapshot.getHubId(), scenario.getName(), actions, actionsById, timestamp);
            }
        }
    }

    private boolean checkConditions(List<ScenarioCondition> conditions, Map<String, SensorStateAvro> sensorsState, Map<Long, Condition> conditionsById) {
        for (ScenarioCondition condition : conditions) {
            String sensorId = condition.getSensorId();
            Condition conditionEntity = conditionsById.get(condition.getConditionId());

            SensorStateAvro sensorState = sensorsState.get(sensorId);
            if (sensorState == null) {
                log.warn("Sensor {} not found in snapshot for scenario {}", sensorId, condition.getScenarioId());
                return false;
            }

            if (conditionEntity == null) {
                log.warn("Condition {} not found", condition.getConditionId());
                return false;
            }

            if (!isConditionMet(sensorState.getData(), conditionEntity)) {
                log.debug("Condition not met for sensor {} in scenario {}", sensorId, condition.getScenarioId());
                return false;
            }
        }
        return true;
    }

    private boolean isConditionMet(Object data, Condition condition) {
        ConditionTypeProto conditionType = parseConditionType(condition.getType());
        ConditionOperationProto operation = parseConditionOperation(condition.getOperation());
        Integer threshold = condition.getValue();

        if (threshold == null || conditionType == null || operation == null) {
            return false;
        }

        return switch (conditionType) {
            case MOTION -> checkMotion((MotionSensorEventAvro) data, operation, threshold);
            case TEMPERATURE -> checkTemperature((ClimateSensorEventAvro) data, operation, threshold);
            case LUMINOSITY -> checkLuminosity((LightSensorEventAvro) data, operation, threshold);
            case SWITCH -> checkSwitch((SwitchSensorEventAvro) data, operation, threshold);
            case CO2LEVEL -> checkCo2Level((ClimateSensorEventAvro) data, operation, threshold);
            case HUMIDITY -> checkHumidity((ClimateSensorEventAvro) data, operation, threshold);
            default -> false;
        };
    }

    private boolean checkMotion(MotionSensorEventAvro data, ConditionOperationProto operation, Integer threshold) {
        int value = data.getMotion() ? 1 : 0;
        return compare(value, operation, threshold);
    }

    private boolean checkTemperature(ClimateSensorEventAvro data, ConditionOperationProto operation, Integer threshold) {
        return compare(data.getTemperatureC(), operation, threshold);
    }

    private boolean checkLuminosity(LightSensorEventAvro data, ConditionOperationProto operation, Integer threshold) {
        return compare(data.getLuminosity(), operation, threshold);
    }

    private boolean checkSwitch(SwitchSensorEventAvro data, ConditionOperationProto operation, Integer threshold) {
        int value = data.getState() ? 1 : 0;
        return compare(value, operation, threshold);
    }

    private boolean checkCo2Level(ClimateSensorEventAvro data, ConditionOperationProto operation, Integer threshold) {
        return compare(data.getCo2Level(), operation, threshold);
    }

    private boolean checkHumidity(ClimateSensorEventAvro data, ConditionOperationProto operation, Integer threshold) {
        return compare(data.getHumidity(), operation, threshold);
    }

    private boolean compare(int actual, ConditionOperationProto operation, Integer threshold) {
        return switch (operation) {
            case EQUALS -> actual == threshold;
            case GREATER_THAN -> actual > threshold;
            case LOWER_THAN -> actual < threshold;
            default -> false;
        };
    }

    private void executeActions(String hubId, String scenarioName, List<ScenarioAction> actions, Map<Long, Action> actionsById, Timestamp timestamp) {
        for (ScenarioAction scenarioAction : actions) {
            Action action = actionsById.get(scenarioAction.getActionId());
            if (action == null) {
                log.warn("Action {} not found", scenarioAction.getActionId());
                continue;
            }

            String sensorId = scenarioAction.getSensorId();

            DeviceActionProto deviceAction = DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(parseActionType(action.getType()))
                    .setValue(action.getValue())
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(deviceAction)
                    .setTimestamp(timestamp)
                    .build();

            try {
                hubRouterClient.handleDeviceAction(request);
                log.info("Action executed: scenario={}, sensor={}, action={}", scenarioName, sensorId, action.getType());
            } catch (Exception e) {
                log.error("Failed to execute action: scenario={}, sensor={}, action={}", scenarioName, sensorId, action.getType(), e);
            }
        }
    }

    private ConditionTypeProto parseConditionType(String type) {
        try {
            return ConditionTypeProto.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.error("Unknown condition type: {}", type);
            return null;
        }
    }

    private ConditionOperationProto parseConditionOperation(String operation) {
        try {
            return ConditionOperationProto.valueOf(operation);
        } catch (IllegalArgumentException e) {
            log.error("Unknown condition operation: {}", operation);
            return null;
        }
    }

    private ActionTypeProto parseActionType(String type) {
        try {
            return ActionTypeProto.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.error("Unknown action type: {}", type);
            return null;
        }
    }

    private Timestamp toTimestampProto(long epochMillis) {
        return Timestamp.newBuilder()
                .setSeconds(epochMillis / 1000)
                .setNanos((int) ((epochMillis % 1000) * 1_000_000))
                .build();
    }
}
