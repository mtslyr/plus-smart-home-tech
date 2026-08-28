package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@NotNull
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceAction {
    @NotNull String sensorId;
    @NotNull ActionType type;
    Integer value;
}
