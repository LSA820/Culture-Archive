package com.example.culture_diary.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class HttpConfig {

    @Bean
    public WebClient webClient(@Value("${kcisa.api.base}") String baseUrl) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl) // 예: https://api.kcisa.kr/openapi/CNV_060
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.USER_AGENT, "CultureDiary/1.0")
                .build();
    }
}



