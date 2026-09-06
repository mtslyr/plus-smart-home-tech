package ru.yandex.practicum.telemetry.collector.service.handler.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorEventAvro;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaEventProducer;

@Component(value = "LIGHT_SENSOR_EVENT")
public class LightSensorEventHandler extends BaseSensorEventHandler<LightSensorEventAvro> {

    public LightSensorEventHandler(KafkaEventProducer producer) {
        super(producer);
    }

    @Override
    protected LightSensorEventAvro mapToAvro(SensorEventProto event) {
        LightSensorProto _event = event.getLightSensorEvent();
        return LightSensorEventAvro.newBuilder()
                .setLinkQuality(_event.getLinkQuality())
                .setLuminosity(_event.getLuminosity())
                .build();
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR_EVENT;
    }
}
