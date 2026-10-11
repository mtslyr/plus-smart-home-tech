package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderData;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.inventory.InventoryClient;
import ru.yandex.practicum.order.feign.inventory.ReleaseRequest;
import ru.yandex.practicum.order.feign.inventory.ReserveRequest;
import ru.yandex.practicum.order.feign.inventory.ReserveResponse;
import ru.yandex.practicum.order.feign.product.ProductClient;
import ru.yandex.practicum.order.feign.product.ProductDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestrationService {
    private final OrderService orderService;
    private final InventoryClient inventoryClient;
    private final ProductClient productClient;

    public OrderDto createOrder(CreateOrderRequest request) {
        log.info("Обработка заказа: позиций={}", request.items().size());

        Map<Long, Integer> productsQty = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            productsQty.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Map<Long, ProductDto> productsData = new LinkedHashMap<>();
        for (Long productId : productsQty.keySet()) {
            productsData.put(productId, loadActiveProduct(productId));
        }

        Map<Long, Integer> reserved = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : productsQty.entrySet()) {
            reserve(entry.getKey(), entry.getValue(), reserved);
        }

        List<OrderItemDto> orderItems = productsData.values()
                .stream()
                .map(product -> new OrderItemDto(
                        null,
                        product.id(),
                        product.name(),
                        reserved.get(product.id()),
                        product.price()
                ))
                .toList();

        OrderData orderData = new OrderData(
                request.customerName(),
                request.customerEmail(),
                orderItems
        );

        try {
            OrderDto order = orderService.create(orderData);
            log.info("Заказ успешно создан: id={}", order.id());
            return order;
        } catch (RuntimeException ex) {
            log.error("Ошибка сохранения заказа, снимаем резерв", ex);
            releaseProducts(reserved);
            throw ex;
        }
    }

    private ProductDto loadActiveProduct(Long productId) {
        ProductDto product;
        try {
            product = productClient.getProductById(productId);
        } catch (FeignException ex) {
            throw mapProductException(ex, productId);
        }

        if (!Boolean.TRUE.equals(product.active())) {
            throw new OrderProcessingException("Товар id=%d снят с продажи".formatted(productId));
        }
        return product;
    }

    private void reserve(Long productId, Integer quantity, Map<Long, Integer> reserved) {
        ReserveResponse reserveResponse;
        try {
            reserveResponse = inventoryClient.reserveStock(new ReserveRequest(productId, quantity));
        } catch (FeignException ex) {
            releaseProducts(reserved);
            throw mapInventoryException(ex, productId);
        }

        if (!reserveResponse.success()) {
            releaseProducts(reserved);
            throw new OrderProcessingException(
                    "Не удалось зарезервировать товар id=%d: %s".formatted(productId, reserveResponse.message())
            );
        }

        reserved.put(productId, quantity);
    }

    private void releaseProducts(Map<Long, Integer> reserved) {
        for (Map.Entry<Long, Integer> entry : reserved.entrySet()) {
            try {
                inventoryClient.releaseStock(new ReleaseRequest(entry.getKey(), entry.getValue()));
            } catch (FeignException ex) {
                log.error("Не удалось снять резерв товара id={} в количестве {}",
                        entry.getKey(), entry.getValue(), ex);
            }
        }
    }

    private OrderProcessingException mapInventoryException(FeignException e, Long productId) {
        if (e.status() == HttpStatus.NOT_FOUND.value()) {
            return new OrderProcessingException(
                    "Складская запись для товара id=%d не найдена".formatted(productId)
            );
        }

        if (e.status() == HttpStatus.CONFLICT.value()) {
            return new OrderProcessingException(
                    "Недостаточно товара id=%d на складе".formatted(productId)
            );
        }

        return new OrderProcessingException(
                "Не удалось зарезервировать товар id=%d".formatted(productId)
        );
    }

    private OrderProcessingException mapProductException(FeignException e, Long productId) {
        if (e.status() == HttpStatus.NOT_FOUND.value()) {
            return new OrderProcessingException(
                    "Товар с id=%d не найден".formatted(productId)
            );
        }

        return new OrderProcessingException("Не удалось получить данные товара id=%d".formatted(productId));
    }
}
