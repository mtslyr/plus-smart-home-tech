package ru.yandex.practicum.inventory.dto;

public record ReleaseResponse(

        boolean success,

        Integer availableQuantity,

        String message) {
}
