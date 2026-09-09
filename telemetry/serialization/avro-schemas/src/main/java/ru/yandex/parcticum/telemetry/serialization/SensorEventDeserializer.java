package ru.yandex.parcticum.telemetry.serialization;

public class SensorEventDeserializer extends BaseAvroDeserializer<ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro> {

    public SensorEventDeserializer() {
        super(ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro.getClassSchema());
    }
}
