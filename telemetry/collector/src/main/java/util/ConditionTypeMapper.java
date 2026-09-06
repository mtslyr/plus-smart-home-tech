package util;

import ru.yandex.practicum.grpc.telemetry.event.ConditionTypeProto;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

public class ConditionTypeMapper {
    public static ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro convert(ConditionTypeProto conditionTypeProto) {
        return switch (conditionTypeProto) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            default -> throw new IllegalArgumentException("Unrecognized condition type: " + conditionTypeProto.name());
        };
    }
}
