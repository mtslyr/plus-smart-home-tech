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
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.ServiceCallResult;
import ru.yandex.practicum.order.feign.inventory.InventoryClient;
import ru.yandex.practicum.order.feign.inventory.ReleaseRequest;
import ru.yandex.practicum.order.feign.inventory.ReserveRequest;
import ru.yandex.practicum.order.feign.inventory.ReserveResponse;
import ru.yandex.practicum.order.feign.product.ProductClient;
import ru.yandex.practicum.order.feign.product.ProductDto;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        Set<String> degradations = new LinkedHashSet<>();

        Map<Long, ProductDto> productsData = new LinkedHashMap<>();
        for (Long productId : productsQty.keySet()) {
            switch (loadActiveProduct(productId)) {
                case ServiceCallResult.Success<ProductDto> success -> productsData.put(productId, success.value());
                case ServiceCallResult.Failure<ProductDto> failure ->
                        throw new OrderProcessingException(failure.message());
                case ServiceCallResult.Degraded<ProductDto> degraded -> degradations.add(degraded.reason());
            }
        }

        Map<Long, Integer> reserved = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : productsQty.entrySet()) {
            switch (reserve(entry.getKey(), entry.getValue())) {
                case ServiceCallResult.Success<ReserveResponse> success -> reserved.put(entry.getKey(), entry.getValue());
                case ServiceCallResult.Failure<ReserveResponse> failure -> {
                    releaseProducts(reserved);
                    throw new OrderProcessingException(failure.message());
                }
                case ServiceCallResult.Degraded<ReserveResponse> degraded -> degradations.add(degraded.reason());
            }
        }

        List<OrderItemDto> orderItems = productsQty.entrySet()
                .stream()
                .map(entry -> {
                    ProductDto product = productsData.get(entry.getKey());
                    return new OrderItemDto(
                            null,
                            entry.getKey(),
                            product == null ? null : product.name(),
                            entry.getValue(),
                            product == null ? null : product.price()
                    );
                })
                .toList();

        OrderStatus status = degradations.isEmpty() ? OrderStatus.CONFIRMED : OrderStatus.PENDING_CONFIRMATION;
        OrderData orderData = new OrderData(
                request.customerName(),
                request.customerEmail(),
                status,
                degradations.isEmpty() ? null : String.join("; ", degradations),
                orderItems
        );

        try {
            OrderDto order = orderService.create(orderData);
            log.info("Заказ создан: id={}, status={}", order.id(), order.status());
            return order;
        } catch (RuntimeException ex) {
            log.error("Ошибка сохранения заказа, снимаем резерв", ex);
            releaseProducts(reserved);
            throw ex;
        }
    }

    private ServiceCallResult<ProductDto> loadActiveProduct(Long productId) {
        ProductDto product;
        try {
            product = productClient.getProductById(productId);
        } catch (ProductServiceUnavailableException ex) {
            return new ServiceCallResult.Degraded<>(
                    "Каталог товаров временно недоступен, товар id=%d не проверен".formatted(productId)
            );
        } catch (FeignException.FeignClientException ex) {
            return new ServiceCallResult.Failure<>(mapProductException(ex, productId));
        }

        if (!Boolean.TRUE.equals(product.active())) {
            return new ServiceCallResult.Failure<>("Товар id=%d снят с продажи".formatted(productId));
        }
        return new ServiceCallResult.Success<>(product);
    }

    private ServiceCallResult<ReserveResponse> reserve(Long productId, Integer quantity) {
        ReserveResponse reserveResponse;
        try {
            reserveResponse = inventoryClient.reserveStock(new ReserveRequest(productId, quantity));
        } catch (InventoryServiceUnavailableException ex) {
            return new ServiceCallResult.Degraded<>(
                    "Склад временно недоступен, товар id=%d не зарезервирован".formatted(productId)
            );
        } catch (FeignException.FeignClientException ex) {
            return new ServiceCallResult.Failure<>(mapInventoryException(ex, productId));
        }

        if (!reserveResponse.success()) {
            return new ServiceCallResult.Failure<>(
                    "Не удалось зарезервировать товар id=%d: %s".formatted(productId, reserveResponse.message())
            );
        }
        return new ServiceCallResult.Success<>(reserveResponse);
    }

    private void releaseProducts(Map<Long, Integer> reserved) {
        for (Map.Entry<Long, Integer> entry : reserved.entrySet()) {
            try {
                inventoryClient.releaseStock(new ReleaseRequest(entry.getKey(), entry.getValue()));
            } catch (RuntimeException ex) {
                log.error("Не удалось снять резерв товара id={} в количестве {}",
                        entry.getKey(), entry.getValue(), ex);
            }
        }
    }

    private String mapInventoryException(FeignException e, Long productId) {
        if (e.status() == HttpStatus.NOT_FOUND.value()) {
            return "Складская запись для товара id=%d не найдена".formatted(productId);
        }

        if (e.status() == HttpStatus.CONFLICT.value()) {
            return "Недостаточно товара id=%d на складе".formatted(productId);
        }

        return "Не удалось зарезервировать товар id=%d".formatted(productId);
    }

    private String mapProductException(FeignException e, Long productId) {
        if (e.status() == HttpStatus.NOT_FOUND.value()) {
            return "Товар с id=%d не найден".formatted(productId);
        }

        return "Не удалось получить данные товара id=%d".formatted(productId);
    }
}
