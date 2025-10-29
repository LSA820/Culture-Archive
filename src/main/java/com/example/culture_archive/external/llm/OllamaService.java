package com.example.culture_archive.external.llm;

import com.example.culture_archive.web.dto.OllamaApiDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:llava:13b}")
    private String model;

    private static final ObjectMapper M = new ObjectMapper();

    private final WebClient client = WebClient.builder()
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();

    /* -------- 공용: 이미지 + 프롬프트 호출 -------- */
    public String analyzeImage(MultipartFile imageFile, String prompt) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(imageFile.getBytes());
        OllamaApiDto.GenerationRequest req =
                new OllamaApiDto.GenerationRequest(model, prompt, List.of(b64), false);
        try {
            OllamaApiDto.GenerationResponse res = client.post()
                    .uri(baseUrl + "/api/generate")
                    .body(Mono.just(req), OllamaApiDto.GenerationRequest.class)
                    .retrieve()
                    .bodyToMono(OllamaApiDto.GenerationResponse.class)
                    .block();
            return (res != null && res.getResponse() != null) ? res.getResponse() : "";
        } catch (WebClientResponseException e) {
            log.warn("Ollama analyzeImage error: {} {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "";
        }
    }

    /* -------- LLaVA: 제목 후보 + 키워드 -------- */
    public record ImageNlpResult(List<String> titleTerms, List<String> keywords) {}

    public ImageNlpResult analyzeImageForTermsAndKeywords(
            MultipartFile imageFile, String ocrText, List<String> allowed) throws IOException {

        String prompt = buildPromptForImage(ocrText, allowed);
        String raw = analyzeImage(imageFile, prompt);

        int s = raw.indexOf('{'), e = raw.lastIndexOf('}');
        if (s < 0 || e <= s) return new ImageNlpResult(List.of(), List.of());
        String json = raw.substring(s, e + 1);

        JsonNode node = M.readTree(json);
        List<String> title = toList(node.get("title_terms"));
        List<String> kwsRaw = toList(node.get("keywords"));

        var allowNorm = allowed.stream().collect(Collectors.toMap(
                a -> a.replaceAll("\\s+","").toLowerCase(), a -> a, (u,v)->u));

        List<String> kws = kwsRaw.stream()
                .map(x -> x.replaceAll("\\s+","").toLowerCase())
                .map(allowNorm::get)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        return new ImageNlpResult(
                title.stream().filter(t -> !t.isBlank()).distinct().toList(),
                kws
        );
    }

    private String buildPromptForImage(String ocrText, List<String> allowed) {
        String allowedList = String.join(", ", allowed);
        return """
You are analyzing a poster image. You also get OCR text extracted from it.

GOALS:
1) Extract 3~10 proper-noun TITLE TERMS likely to be the event/work/exhibition name or venue.
2) Select KEYWORDS ONLY from the allowed list.

PRIORITY for keywords:
- Prefer terms that appear in BOTH OCR_TEXT and IMAGE analysis.
- If not enough overlap, fill remaining with best-fitting items from the allowed list.

RULES:
- Output MUST be a single JSON object, nothing else.
- No code fences, no explanations.
- title_terms: strings, unique, trimmed.
- keywords: choose ONLY from the allowed list below.

ALLOWED_KEYWORDS:
%s

OCR_TEXT:
%s

OUTPUT FORMAT:
{"title_terms":["체인소맨","전시","서울"], "keywords":["애니메이션","전시","서울"]}
""".formatted(allowedList, ocrText);
    }

    private static List<String> toList(JsonNode n) {
        if (n == null || !n.isArray()) return List.of();
        return M.convertValue(n, M.getTypeFactory()
                .constructCollectionType(List.class, String.class));
    }

    /* -------- 텍스트만으로 허용 키워드 선택 (기존 기능 유지) -------- */
    public List<String> suggestKeywordsFromText(String text, List<String> allowed) {
        if (text == null || text.isBlank() || allowed == null || allowed.isEmpty()) return List.of();

        String allowedList = String.join(", ", allowed);
        String prompt = """
You are a classifier. Choose ONLY from the allowed keywords.
Output MUST be a JSON array of strings. No explanations.

Rules:
- Use ONLY words that appear in the allowed list
- Pick 3~8 keywords that best describe the text
- No duplicates

[Allowed keywords]
%s

[Text]
%s

[Output format]
["키워드1","키워드2",...]
""".formatted(allowedList, text);

        var req = new OllamaApiDto.GenerationRequest(model, prompt, List.of(), false);

        try {
            OllamaApiDto.GenerationResponse res = client.post()
                    .uri(baseUrl + "/api/generate")
                    .body(Mono.just(req), OllamaApiDto.GenerationRequest.class)
                    .retrieve()
                    .bodyToMono(OllamaApiDto.GenerationResponse.class)
                    .block();

            String raw = (res == null) ? null : res.getResponse();
            if (raw == null || raw.isBlank()) return List.of();

            int s = raw.indexOf('['), e = raw.lastIndexOf(']');
            if (s < 0 || e <= s) return List.of();

            String jsonArr = raw.substring(s, e + 1);
            JsonNode node = M.readTree(jsonArr);
            if (!node.isArray()) return List.of();

            var allowNorm = allowed.stream().collect(Collectors.toMap(
                    a -> a.replaceAll("\\s+", "").toLowerCase(),
                    a -> a, (u, v) -> u
            ));

            List<String> rawList = M.convertValue(
                    node, M.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return rawList.stream()
                    .map(String::trim)
                    .map(s2 -> s2.replaceAll("\\s+", "").toLowerCase())
                    .map(norm -> {
                        if (allowNorm.containsKey(norm)) return allowNorm.get(norm);
                        return allowNorm.keySet().stream()
                                .filter(a -> norm.contains(a) || a.contains(norm))
                                .findFirst()
                                .map(allowNorm::get)
                                .orElse(null);
                    })
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .limit(12)
                    .toList();

        } catch (Exception ex) {
            log.warn("Ollama suggestKeywordsFromText 실패: {}", ex.toString());
            return List.of();
        }
    }
}
