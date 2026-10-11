package ru.yandex.practicum.order.dto;

import java.util.List;

public record OrderData(
        String customerName,
        String customerEmail,
        List<OrderItemDto> items
) {
}
