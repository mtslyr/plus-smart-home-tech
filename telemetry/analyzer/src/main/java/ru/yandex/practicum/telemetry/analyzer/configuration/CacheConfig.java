package ru.yandex.practicum.telemetry.analyzer.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("scenarios");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(java.time.Duration.ofMinutes(5)));
        return manager;
    }
}
