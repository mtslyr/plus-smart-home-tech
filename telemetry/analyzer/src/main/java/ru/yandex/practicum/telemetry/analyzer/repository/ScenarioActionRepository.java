package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioAction;

import java.util.Collection;
import java.util.List;

public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, Long> {
    List<ScenarioAction> findByScenarioId(Long scenarioId);
    List<ScenarioAction> findByScenarioIdIn(Collection<Long> scenarioIds);
}
