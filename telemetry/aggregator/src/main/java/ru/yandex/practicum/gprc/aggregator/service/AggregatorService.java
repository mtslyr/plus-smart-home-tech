package ru.yandex.practicum.gprc.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        log.info("Update state for event {} with hubId: {}", event.getId(), event.getHubId());
        SensorsSnapshotAvro snapshotAvro = snapshots.getOrDefault(event.getHubId(), SensorsSnapshotAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(Instant.ofEpochMilli(event.getTimestamp()))
                .setSensorsState(new HashMap<>())
                .build());


        if (snapshotAvro.getSensorsState().containsKey(event.getId())) {
            SensorStateAvro oldState = snapshotAvro.getSensorsState().get(event.getId());

            if (event.getTimestamp() < oldState.getTimestamp().toEpochMilli()
                    || Objects.equals(oldState.getData(), event.getPayload())) {
                log.info("Nothing to update for eventId {}", event.getId());
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(Instant.ofEpochMilli(event.getTimestamp()))
                .setData(event.getPayload())
                .build();

        Map<String, SensorStateAvro> sensorsState = new HashMap<>(snapshotAvro.getSensorsState());
        sensorsState.put(event.getId(), newState);

        snapshotAvro.setSensorsState(sensorsState);
        snapshots.put(event.getHubId(), snapshotAvro);

        log.info("Snapshot for hub {} updated with sensor {}", event.getHubId(), event.getId());
        return Optional.of(snapshotAvro);
    }
}
