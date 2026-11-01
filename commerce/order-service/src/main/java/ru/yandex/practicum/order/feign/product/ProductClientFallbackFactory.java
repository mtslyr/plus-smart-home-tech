package ru.yandex.practicum.order.feign.product;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            if (cause instanceof FeignException.FeignClientException clientException) {
                throw clientException;
            }

            log.warn("product-service недоступен при запросе товара id={}: {}", productId, cause.toString());
            throw new ProductServiceUnavailableException(productId, cause);
        };
    }
}
