package com.example.culture_archive.external;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcisaClient {

    private final @org.springframework.beans.factory.annotation.Qualifier("kcisaWebClient")
    WebClient webClient;

    @Value("${kcisa.api.key}")
    private String apiKey;

    public List<KcisaXml.Event> searchEvents(String dtype, String title,
                                             Integer pageNo, Integer numOfRows) {

        // 1) 전달 문자열의 코드포인트 로그
        log.info("KCISA cps dtype={}, title={}",
                toCps(dtype), toCps(title)); // 예: U+C804,U+C2DC

        // 2) URI를 실제로 찍기 위해 build 결과를 보관
        final String[] builtUrl = new String[1];
        Function<UriBuilder, URI> uriFn = uri -> {
            URI u = uri.path("/request")
                    .queryParam("serviceKey", apiKey == null ? "" : apiKey.trim())
                    .queryParam("dtype", dtype)
                    .queryParam("title", title)
                    .queryParam("pageNo", Optional.ofNullable(pageNo).orElse(1))
                    .queryParam("numOfRows", Optional.ofNullable(numOfRows).orElse(10))
                    .build();
            builtUrl[0] = u.toString();     // 퍼센트 인코딩 형태 확인용
            return u;
        };

        log.info("[KCISA CALL] URI={}", (Object) builtUrl[0]); // 첫 호출 전 null일 수 있음

        try {
            String xmlString = webClient.get()
                    .uri(uriFn)
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .blockOptional()
                    .map(resp -> {
                        log.info("[KCISA CALL] finalURI={}", builtUrl[0]); // 여기에서 찍어야 값 있음
                        log.info("[KCISA HTTP] status={} len={}",
                                resp.getStatusCode(), resp.getBody() == null ? 0 : resp.getBody().length());
                        return resp.getBody();
                    })
                    .orElse("");

            log.info("[KCISA CALL] finalURI={}", builtUrl[0]); // 실제 최종 URI
            if (xmlString.isBlank()) {
                log.warn("[KCISA] blank body (timeout or upstream issue)");
                return List.of();
            }

            XmlMapper mapper = new XmlMapper();
            KcisaXml xml = mapper.readValue(xmlString, KcisaXml.class);

            if (xml == null || xml.getBody() == null
                    || xml.getBody().getItems() == null
                    || xml.getBody().getItems().getItem() == null) {
                log.warn("[KCISA PARSE] no items");
                return List.of();
            }

            List<KcisaXml.Event> items = xml.getBody().getItems().getItem();
            log.info("[KCISA PARSE] items={}", items.size());
            return items;

        } catch (WebClientResponseException e) {
            log.warn("[KCISA HTTP] {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (IOException e) {
            log.warn("[KCISA XML] parse error: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.warn("[KCISA ERROR] {}", e.getMessage());
            return List.of();
        }
    }

    private static String toCps(String s){
        if (s == null) return "null";
        return s.chars()
                .mapToObj(cp -> String.format("U+%04X", cp))
                .reduce((a,b)->a+","+b).orElse("");
    }
}
