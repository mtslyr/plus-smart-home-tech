package ru.yandex.practicum.telemetry.collector.model;

import org.apache.avro.generic.GenericEnumSymbol;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class EnumMapper {
    public static <E extends Enum<E>, A extends GenericEnumSymbol> A map(E enumValue, Class<A> avroClass) {
        if (enumValue == null) {
            return null;
        }

        A[] values = avroClass.getEnumConstants();

        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Avro class has no constants");
        }

        return Arrays.stream(values)
                .filter(a -> a.toString().equals(enumValue.name()))
                .findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException("No mapping for %s in %s"
                                .formatted(enumValue.name(), avroClass.getClass().getSimpleName()))
                );
    }
}
