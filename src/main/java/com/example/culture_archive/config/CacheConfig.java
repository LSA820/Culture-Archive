package com.example.culture_archive.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public Caffeine<Object,Object> caffeine() {
        return Caffeine.newBuilder()
                .recordStats()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(5));

    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object,Object> caffeine) {
        CaffeineCacheManager m = new CaffeineCacheManager("kcisa");
        m.setCaffeine(caffeine);
        return m;
    }
}
