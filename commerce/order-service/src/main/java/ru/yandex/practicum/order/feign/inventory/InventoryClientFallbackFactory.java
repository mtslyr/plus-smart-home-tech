package ru.yandex.practicum.order.feign.inventory;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {
    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                throw toException("резервировании", request.productId());
            }

            @Override
            public ReleaseResponse releaseStock(ReleaseRequest request) {
                throw toException("снятии резерва", request.productId());
            }

            private RuntimeException toException(String operation, Long productId) {
                if (cause instanceof FeignException.FeignClientException clientException) {
                    return clientException;
                }

                log.warn("inventory-service недоступен при {} товара id={}: {}", operation, productId, cause.toString());
                return new InventoryServiceUnavailableException(productId, cause);
            }
        };
    }
}
