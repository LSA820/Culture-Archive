package com.example.culture_archive.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kcisaWebClient(WebClient.Builder builder) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)   // 연결 10초
                .responseTimeout(Duration.ofSeconds(30))               // 응답 전체 30초
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30))  // 읽기 30초
                                .addHandlerLast(new WriteTimeoutHandler(30)) // 쓰기 30초
                );

        return builder
                .baseUrl("https://api.kcisa.kr/openapi/CNV_060")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (CultureArchive)")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}
