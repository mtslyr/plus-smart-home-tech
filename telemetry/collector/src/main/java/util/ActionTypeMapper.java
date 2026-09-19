package util;

import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;

public class ActionTypeMapper {


    public static ActionTypeAvro convert(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case INVERSE -> ActionTypeAvro.INVERSE;
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            default -> throw new IllegalArgumentException("Unrecognized action type: " + actionTypeProto.name());
        };
    }
}
