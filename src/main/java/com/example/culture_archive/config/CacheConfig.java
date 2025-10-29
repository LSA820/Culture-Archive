package com.example.culture_archive.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig { // KCISA 이벤트 목록 캐싱

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        // 사용 캐시 이름 일괄 지정
        mgr.setCacheNames(List.of("kcisa", "homeUpcoming", "upcomingEvents", "eventsCache"));
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(Duration.ofMinutes(30))
                        .recordStats()
        );
        return mgr;
    }
}
