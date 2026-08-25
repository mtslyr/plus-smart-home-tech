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
public class MotionSensorEvent extends SensorEvent {
    @NotNull Integer linkQuality;
    @NotNull Boolean motion;
    @NotNull Integer voltage;
    SensorEventType eventType = SensorEventType.MOTION_SENSOR_EVENT;
}
