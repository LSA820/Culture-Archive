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
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig { // KCISA 호출용 WebClient

    @Bean("kcisaWebClient")
    public WebClient kcisaWebClient(WebClient.Builder builder) {
        HttpClient http = HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(20))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(20, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(20, TimeUnit.SECONDS)));

        return builder
                .baseUrl("https://api.kcisa.kr/openapi/CNV_060")
                .defaultHeaders(h -> {
                    h.set(HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                    h.set(HttpHeaders.ACCEPT_ENCODING, "identity"); // gzip 비활성화
                    h.set(HttpHeaders.CONNECTION, "close");          // 요청 단위 연결 종료
                    h.setAccept(List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.ALL));
                })
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}
