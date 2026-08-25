package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
@NotNull
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScenarioAddedEvent extends HubEvent {
    @NotBlank
    String name;

    @NotEmpty
    List<ScenarioCondition> conditions;

    @NotEmpty
    List<DeviceAction> actions;

    HubEventType eventType = HubEventType.SCENARIO_ADDED;
}
