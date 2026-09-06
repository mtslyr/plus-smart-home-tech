package util;

import ru.yandex.practicum.grpc.telemetry.event.DeviceTypeProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;


public class DeviceTypeMapper{

    public static ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro convert(DeviceTypeProto typeProto) {
        return switch (typeProto) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            default -> throw new IllegalArgumentException("Unrecognized device type: " + typeProto.name());
        };
    }
}
