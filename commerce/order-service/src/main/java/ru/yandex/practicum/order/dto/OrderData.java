package ru.yandex.practicum.order.dto;

import ru.yandex.practicum.order.entity.OrderStatus;

import java.util.List;

public record OrderData(
        String customerName,
        String customerEmail,
        OrderStatus status,
        String statusDetails,
        List<OrderItemDto> items
) {
}
