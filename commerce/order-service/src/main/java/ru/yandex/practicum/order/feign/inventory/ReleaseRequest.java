package ru.yandex.practicum.order.feign.inventory;

public record ReleaseRequest(
        Long productId,
        Integer quantity) {
}
