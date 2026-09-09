package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioCondition;

import java.util.Collection;
import java.util.List;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, Long> {
    List<ScenarioCondition> findByScenarioId(Long scenarioId);
    List<ScenarioCondition> findByScenarioIdIn(Collection<Long> scenarioIds);
}
