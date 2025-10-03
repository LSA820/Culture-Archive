package com.example.culture_archive.external;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcisaClient {

    @Qualifier("kcisaWebClient")
    private final WebClient webClient;

    @Value("${kcisa.api.key}")
    private String apiKey;

    public Mono<List<KcisaXml.Event>> searchEvents(String dtype, String searchWord,
                                                   Integer pageNo, Integer numOfRows) {
        final String[] builtUrl = new String[1];

        return webClient.get()
                .uri(uri -> {
                    var ub = uri.path("/request")
                            .queryParam("serviceKey", apiKey == null ? "" : apiKey.trim())
                            .queryParam("dtype", dtype)
                            .queryParam("pageNo", Optional.ofNullable(pageNo).orElse(1))
                            .queryParam("numOfRows", Optional.ofNullable(numOfRows).orElse(20));
                    if (searchWord != null && !searchWord.isBlank()) {
                        ub = ub.queryParam("title", searchWord);
                    }
                    URI u = ub.build();
                    builtUrl[0] = u.toString();
                    return u;
                })
                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(xmlString -> {
                    if (xmlString.isBlank()) {
                        log.warn("[KCISA] blank body from {}", builtUrl[0]);
                        return Collections.<KcisaXml.Event>emptyList();
                    }
                    try {
                        var mapper = new XmlMapper();
                        var xml = mapper.readValue(xmlString, KcisaXml.class);
                        var items = Optional.ofNullable(xml.getBody())
                                .map(KcisaXml.Body::getItems)
                                .map(KcisaXml.Items::getItem)
                                .orElse(Collections.<KcisaXml.Event>emptyList()); // 타입 명시
                        log.info("[KCISA PARSE] items={} (from {})", items.size(), builtUrl[0]);
                        return items;
                    } catch (Exception e) {
                        log.warn("[KCISA XML PARSE ERROR] {} from {}", e.getMessage(), builtUrl[0]);
                        return Collections.<KcisaXml.Event>emptyList(); // 타입 명시
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("[KCISA TIMEOUT/ERROR] {} - {}", builtUrl[0], ex.getMessage());
                    return Mono.just(Collections.<KcisaXml.Event>emptyList()); // 타입 명시
                });
    }
}