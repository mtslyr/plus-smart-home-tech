package util;

import ru.yandex.practicum.grpc.telemetry.event.ConditionOperationProto;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;

public class OperationTypeMapper {

    public static ConditionOperationAvro convert(ConditionOperationProto conditionOperationProto) {
        return switch (conditionOperationProto) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
            default -> throw new IllegalArgumentException("Unrecognized operation type: " + conditionOperationProto.name());
        };
    }
}

