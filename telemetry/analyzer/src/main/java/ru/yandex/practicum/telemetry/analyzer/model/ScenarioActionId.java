package ru.yandex.practicum.telemetry.analyzer.model;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioActionId implements Serializable {

    private Long scenarioId;
    private String sensorId;
    private Long actionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioActionId that = (ScenarioActionId) o;
        return Objects.equals(scenarioId, that.scenarioId) &&
                Objects.equals(sensorId, that.sensorId) &&
                Objects.equals(actionId, that.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioId, sensorId, actionId);
    }
}
