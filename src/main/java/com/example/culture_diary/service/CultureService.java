// service/CultureService.java
package com.example.culture_diary.service;

import com.example.culture_diary.web.KcisaXml;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CultureService {

    private final WebClient webClient;

    @Value("${kcisa.api.key}")
    private String apiKey;

    public List<KcisaXml.Event> searchEvents(String dtype, String title,
                                             Integer pageNo, Integer numOfRows) {
        Function<UriBuilder, URI> uriFn = uri -> uri.path("/request")
                .queryParam("serviceKey", apiKey == null ? "" : apiKey.trim())
                .queryParam("dtype", dtype)
                .queryParam("title", title)
                .queryParam("pageNo", Optional.ofNullable(pageNo).orElse(1))
                .queryParam("numOfRows", Optional.ofNullable(numOfRows).orElse(10))
                .build();

        try {
            String xmlString = webClient.get()
                    .uri(uriFn)
                    .accept(MediaType.TEXT_XML)            // 서버가 text/xml로 응답
                    .retrieve()
                    .bodyToMono(String.class)              // 문자열로 받고
                    .block();                              // (동기)

            if (xmlString == null || xmlString.isBlank()) {
                return List.of();
            }

            XmlMapper mapper = new XmlMapper();
            KcisaXml xml = mapper.readValue(xmlString, KcisaXml.class);  // ← 여기서 예외 발생 가능

            if (xml == null || xml.getBody() == null
                    || xml.getBody().getItems() == null
                    || xml.getBody().getItems().getItem() == null) {
                return List.of();
            }
            return xml.getBody().getItems().getItem();

        } catch (WebClientResponseException e) {
            // HTTP 오류 (예: 403/504) 시
            System.err.println("[KCISA HTTP] " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return List.of();

        } catch (IOException e) {
            // XML 파싱 실패 시
            System.err.println("[KCISA XML PARSE ERROR] " + e.getMessage());
            return List.of();

        } catch (Exception e) {
            // 그 외 예외
            System.err.println("[KCISA UNKNOWN ERROR] " + e.getMessage());
            return List.of();
        }
    }
}
