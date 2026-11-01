package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.OrderData;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<OrderDto> getAll() {
        return orderRepository.findAll().stream().map(OrderMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        return OrderMapper.toDto(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getByEmail(String email) {
        return orderRepository.findAllByCustomerEmail(email).stream().map(OrderMapper::toDto).toList();
    }

    @Transactional
    public OrderDto create(OrderData orderData) {
        Order order = Order.builder()
                .customerName(orderData.customerName())
                .customerEmail(orderData.customerEmail())
                .status(orderData.status().name())
                .statusDetails(orderData.statusDetails())
                .createdAt(LocalDateTime.now())
                .build();

        // Если цена хотя бы одной позиции неизвестна (каталог недоступен), итог не считаем
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDto orderItemDto : orderData.items()) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(orderItemDto.productId())
                    .productName(orderItemDto.productName())
                    .quantity(orderItemDto.quantity())
                    .price(orderItemDto.price())
                    .build();
            order.getItems().add(item);
            total = total == null || orderItemDto.price() == null
                    ? null
                    : total.add(orderItemDto.price().multiply(BigDecimal.valueOf(orderItemDto.quantity())));
        }
        order.setTotalPrice(total);

        return OrderMapper.toDto(orderRepository.save(order));
    }

    private Order getEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ с id=" + id + " не найден"));
    }
}
