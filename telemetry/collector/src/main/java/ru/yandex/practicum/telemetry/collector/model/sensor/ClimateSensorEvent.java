package ru.yandex.practicum.telemetry.collector.model.sensor;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString(callSuper = true)
@NotNull
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClimateSensorEvent extends SensorEvent {
    @NotNull Integer temperatureC;
    @NotNull Integer humidity;
    @NotNull Integer co2Level;
    SensorEventType eventType = SensorEventType.CLIMATE_SENSOR_EVENT;
}
