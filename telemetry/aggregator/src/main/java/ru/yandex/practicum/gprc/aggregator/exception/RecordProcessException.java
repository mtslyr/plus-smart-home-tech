package ru.yandex.practicum.gprc.aggregator.exception;

import lombok.Getter;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Getter
@Setter
public class RecordProcessException extends RuntimeException {
    private ConsumerRecord<String, SpecificRecordBase> failed;

    public RecordProcessException(String message, Throwable t) {
        super(message, t);
    }
}
