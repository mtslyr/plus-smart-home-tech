package ru.yandex.practicum.telemetry.analyzer.model;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioConditionId implements Serializable {

    private Long scenarioId;
    private String sensorId;
    private Long conditionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioConditionId that = (ScenarioConditionId) o;
        return Objects.equals(scenarioId, that.scenarioId) &&
                Objects.equals(sensorId, that.sensorId) &&
                Objects.equals(conditionId, that.conditionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenarioId, sensorId, conditionId);
    }
}
