package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Cacheable(value = "scenarios", key = "#hubId")
    public List<Scenario> findByHubId(String hubId) {
        return scenarioRepository.findByHubId(hubId);
    }

    public Map<Long, List<ScenarioCondition>> findConditionsByScenarioIds(Collection<Long> scenarioIds) {
        return scenarioConditionRepository.findByScenarioIdIn(scenarioIds)
                .stream()
                .collect(Collectors.groupingBy(c -> c.getScenarioId()));
    }

    public Map<Long, List<ScenarioAction>> findActionsByScenarioIds(Collection<Long> scenarioIds) {
        return scenarioActionRepository.findByScenarioIdIn(scenarioIds)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getScenarioId()));
    }

    public Map<Long, Condition> findConditionsByIds(Collection<Long> ids) {
        return conditionRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Condition::getId, c -> c));
    }

    public Map<Long, Action> findActionsByIds(Collection<Long> ids) {
        return actionRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Action::getId, a -> a));
    }
}
