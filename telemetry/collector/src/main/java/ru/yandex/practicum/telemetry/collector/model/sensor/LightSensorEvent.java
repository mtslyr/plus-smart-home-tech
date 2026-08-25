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
public class LightSensorEvent extends SensorEvent {
    Integer linkQuality;
    Integer luminosity;
    SensorEventType eventType = SensorEventType.LIGHT_SENSOR_EVENT;
}
