
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
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        // 필요한 캐시 미리 생성 (둘 다 등록)
        mgr.setCacheNames(List.of("kcisa", "homeUpcoming"));

        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(Duration.ofMinutes(30))
                        .recordStats() // ← metrics 경고 제거
        );
        return mgr;
    }
}
