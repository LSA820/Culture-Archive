package com.example.culture_archive.external;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcisaClient {

    private final @org.springframework.beans.factory.annotation.Qualifier("kcisaWebClient")
    WebClient webClient;

    @Value("${kcisa.api.key}")
    private String apiKey;

    @Cacheable(
            cacheNames = "kcisa",
            key = "T(java.util.Objects).hash(#dtype,#searchWord,#pageNo,#numOfRows)",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<KcisaXml.Event> searchEvents(String dtype, String searchWord,
                                             Integer pageNo, Integer numOfRows) {
        final String[] builtUrl = new String[1];

        try {
            String xmlString = webClient.get()
                    .uri(uri -> {
                        var ub = uri.path("/request")
                                .queryParam("serviceKey", apiKey == null ? "" : apiKey.trim())
                                .queryParam("dtype", dtype)
                                .queryParam("pageNo", Optional.ofNullable(pageNo).orElse(1))
                                .queryParam("numOfRows", Optional.ofNullable(numOfRows).orElse(30));

                        // 검색어 있으면 파라미터 추가
                        if (searchWord != null && !searchWord.isBlank()) {
                            ub = ub.queryParam("title", searchWord); // KCISA는 보통 title/searchWrd 중 하나
                        }

                        URI u = ub.build();
                        builtUrl[0] = u.toString(); // 최종 URI 저장
                        return u;
                    })
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(12))
                    .block()
                    .getBody();

            log.info("[KCISA CALL] finalURI={}", builtUrl[0]);

            if (xmlString == null || xmlString.isBlank()) {
                log.warn("[KCISA] blank body");
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
