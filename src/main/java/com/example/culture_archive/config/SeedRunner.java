package com.example.culture_archive.config;

import com.example.culture_archive.service.keyword.KeywordService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedRunner { // 부팅 시 키워드 화이트리스트 1회 시드
    @Bean
    ApplicationRunner seedKeywords(KeywordService ks) {
        return args -> ks.seedWhitelistIfEmpty(); // 앱 부팅 시 1회 보장
    }
}
