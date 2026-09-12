package ru.yandex.practicum.telemetry.collector.service.handler.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorEventAvro;
import ru.yandex.practicum.telemetry.collector.kafka.KafkaEventProducer;

@Component(value = "CLIMATE_SENSOR_EVENT")
public class ClimateSensorEventHandler extends BaseSensorEventHandler<ClimateSensorEventAvro> {

    public ClimateSensorEventHandler(KafkaEventProducer producer) {
        super(producer);
    }

    @Override
    protected ClimateSensorEventAvro mapToAvro(SensorEventProto event) {
        ClimateSensorProto _event = event.getClimateSensorEvent();
        return ClimateSensorEventAvro.newBuilder()
                .setCo2Level(_event.getCo2Level())
                .setHumidity(_event.getHumidity())
                .setTemperatureC(_event.getTemperatureC())
                .build();
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR_EVENT;
    }
}
