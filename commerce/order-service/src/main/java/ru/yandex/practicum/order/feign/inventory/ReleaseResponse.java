package ru.yandex.practicum.order.feign.inventory;

public record ReleaseResponse(
        boolean success,

        Integer availableQuantity,

        String message) {
}
