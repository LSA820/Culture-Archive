// src/main/java/com/example/culture_archive/service/InterparkCrawlService.java
package com.example.culture_archive.external.interpark;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.repository.EventRepository;
import com.example.culture_archive.util.RegionClassifier;
import com.example.culture_archive.web.dto.InterparkApiDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterparkCrawlService {

    private final EventRepository eventRepository;
    private final RegionClassifier regionClassifier;

    private final WebClient webClient = WebClient.builder().build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 필요한 장르만 */
    private enum Genre {
        MUSICAL("MUSICAL", "뮤지컬"),
        EXHIBIT("EXHIBIT", "전시"),
        CONCERT("CONCERT", "콘서트"),
        PLAY("PLAY", "연극");
        final String code; final String defaultType;
        Genre(String code, String defaultType){ this.code=code; this.defaultType=defaultType; }
    }

    private static final String BASE =
            "https://tickets.interpark.com/contents/api/goods/genre?genre=%s&page=%d&pageSize=%d&sort=DAILY_RANKING";

    private static final int PAGE_SIZE = 25;
    private static final int MAX_PAGES  = 50;     // 페이지 상한
    private static final int DAYS_BACK  = 365;    // 과거 수집 범위
    private static final DateTimeFormatter RAW8 = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 공개 호출: 네 장르 모두 수집 */
    @Transactional
    public void crawlAllSelectedGenres() {
        crawlGenres(EnumSet.of(Genre.MUSICAL, Genre.EXHIBIT, Genre.CONCERT, Genre.PLAY), DAYS_BACK);
    }

    /* 내부 구현 */

    private void crawlGenres(Set<Genre> genres, int daysBack){
        int total = 0;
        for (Genre g : genres) total += crawlOneGenre(g, daysBack);
        log.info("[Interpark] all genres done, added {}", total);
    }

    /** 랭킹 API 다중 페이지 순회. 종료 조건: 페이지 소진 또는 종료일이 컷오프 이전만 남음 */
    private int crawlOneGenre(Genre genre, int daysBack){
        LocalDate cutoff = LocalDate.now().minusDays(daysBack);
        int saved = 0;

        for (int page=1; page<=MAX_PAGES; page++){
            String url = String.format(BASE, genre.code, page, PAGE_SIZE);
            List<InterparkApiDto.GoodsItem> items = fetchByUrl(url);
            if (items.isEmpty()) break;

            boolean reachedOld = false;

            for (InterparkApiDto.GoodsItem it : items) {
                try {
                    Event ev = mapToEvent(it, genre.defaultType);
                    if (ev == null) continue;

                    // 과거 컷오프 체크(endDate 기준, 없으면 통과)
                    if (ev.getEndDate()!=null && ev.getEndDate().isBefore(cutoff)) {
                        reachedOld = true;
                        continue;
                    }

                    // 중복 방지: 제목+기간
                    if (!eventRepository.existsByTitleAndPeriod(ev.getTitle(), ev.getPeriod())) {
                        eventRepository.save(ev);
                        saved++;
                    }
                } catch (Exception e) {
                    log.error("[Interpark] {} item fail name='{}' err={}", genre.code, it.getGoodsName(), e.getMessage());
                }
            }

            log.info("[Interpark] {} page {} done, added {}", genre.code, page, saved);
            if (reachedOld) break; // 이 페이지부터 과거만 보이면 중단
        }
        log.info("[Interpark] {} total added {}", genre.code, saved);
        return saved;
    }

    private List<InterparkApiDto.GoodsItem> fetchByUrl(String url){
        try {
            String json = webClient.get().uri(url).retrieve().bodyToMono(String.class).block();
            if (json == null || json.isBlank()) return Collections.emptyList();
            return parseFlexible(json);
        } catch (WebClientResponseException e) {
            log.error("[Interpark] http {} {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[Interpark] fetch error {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 응답 형태: [..] | { "exhibit":[..] } | { "data":{"list":[..]} } | { "list":[..] } */
    private List<InterparkApiDto.GoodsItem> parseFlexible(String json) throws Exception {
        JsonNode root = MAPPER.readTree(json);
        List<InterparkApiDto.GoodsItem> out = new ArrayList<>();

        JsonNode arr = null;
        if (root.isArray())                      arr = root;
        else if (root.path("exhibit").isArray()) arr = root.path("exhibit");
        else if (root.path("data").path("list").isArray()) arr = root.path("data").path("list");
        else if (root.path("list").isArray())    arr = root.path("list");

        if (arr == null) return Collections.emptyList();
        for (JsonNode n : arr) out.add(MAPPER.convertValue(n, InterparkApiDto.GoodsItem.class));
        return out;
    }

    /** DTO → Event */
    private Event mapToEvent(InterparkApiDto.GoodsItem d, String fallbackType) {
        if (blank(d.getGoodsName())) return null;

        String rawS = trim(d.getPlayStartDate());
        String rawE = trim(d.getPlayEndDate());

        LocalDate s = parseDate(rawS);
        LocalDate e = parseDate(rawE);

        String dispS = toDisp(rawS);
        String dispE = toDisp(rawE);
        String period = buildPeriod(dispS, dispE);

        String img = normalizeImg(d.getImageUrl());
        String site = def(d.getPlaceName(), "미정");
        String region = def(regionClassifier.classify(site), "기타");
        String type = def(d.getKindOfGoodsName(), fallbackType);
        String source = blank(d.getGoodsCode()) ? null : "https://tickets.interpark.com/goods/" + d.getGoodsCode();

        return Event.builder()
                .title(d.getGoodsName())
                .type(type)
                .eventSite(site)
                .period(period)
                .imageUrl(img)
                .sourceUrl(source)
                .startDate(s)
                .endDate(e)
                .region(region)
                .build();
    }

    /* helpers */
    private LocalDate parseDate(String v){
        if (blank(v)) return null;
        String s=v.trim();
        try{
            if (s.matches("\\d{8}")) return LocalDate.parse(s, RAW8);
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(s);
        }catch(Exception ignore){}
        return null;
    }
    private String toDisp(String v){
        if (blank(v)) return null;
        String s=v.trim();
        if (s.matches("\\d{8}")) return s.substring(0,4)+"-"+s.substring(4,6)+"-"+s.substring(6,8);
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return s;
        return null;
    }
    private String buildPeriod(String s, String e){
        if (s==null && e==null) return "";
        if (s==null) return " ~ " + e;
        if (e==null) return s + " ~ ";
        return s + " ~ " + e;
    }
    private String normalizeImg(String url){ if (blank(url)) return null; return url.startsWith("//")?"https:"+url:url; }
    private boolean blank(String s){ return s==null || s.isBlank(); }
    private String trim(String s){ return blank(s)?null:s.trim(); }
    private String def(String s,String d){ return blank(s)?d:s; }
}
