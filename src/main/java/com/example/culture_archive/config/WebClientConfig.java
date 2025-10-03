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
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean("kcisaWebClient")
    public WebClient kcisaWebClient(WebClient.Builder builder) {
        HttpClient http = HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(20))
                // 특별 설정 무리 안 줌 (compress/keepAlive 커스텀 제거)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(20, java.util.concurrent.TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(20, java.util.concurrent.TimeUnit.SECONDS))
                );

        return builder
                .baseUrl("https://api.kcisa.kr/openapi/CNV_060")
                .defaultHeaders(h -> {
                    h.set(HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                    // 핵심 1: 서버가 gzip 응답에서 멈추는 경우가 있어 'identity' 강제
                    h.set(HttpHeaders.ACCEPT_ENCODING, "identity");
                    // 핵심 2: 일부 WAF/게이트웨이가 커넥션 유지 시 응답을 안 주는 경우 → 명시적으로 끊기
                    h.set(HttpHeaders.CONNECTION, "close");
                    // Accept는 넉넉하게
                    h.setAccept(java.util.List.of(
                            MediaType.APPLICATION_XML,
                            MediaType.TEXT_XML,
                            MediaType.ALL
                    ));
                    // GET에 Content-Type 절대 넣지 않음
                })
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}


