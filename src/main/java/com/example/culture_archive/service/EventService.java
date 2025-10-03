package com.example.culture_archive.service;

import com.example.culture_archive.external.KcisaClient;
import com.example.culture_archive.external.KcisaXml;
import com.example.culture_archive.util.DateRange;
import com.example.culture_archive.util.PeriodParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final KcisaClient kcisaClient;

    private static final List<String> DTYPE_CANDIDATES =
            List.of("전시", "공연", "뮤지컬", "국악"); // 이 부분을 수정하신 것으로 보입니다.

    @Cacheable(
            cacheNames = "homeUpcoming",
            key = "#sort+'|'+#dir+'|'+#page+'|'+#size",
            unless = "#result == null || #result.content().isEmpty()"
    )
    public Slice<KcisaXml.Event> getUpcoming(String sort, String dir, int page, int size) {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(30);

        // 병렬 처리 로직을 제거하고, 순차 처리(for 루프)로 변경합니다.
        List<KcisaXml.Event> bucket = new ArrayList<>();
        for (String dtype : DTYPE_CANDIDATES) {
            try {
                // API 서버에 부담을 주지 않기 위해 각 요청 사이에 0.3초의 지연(delay)을 줍니다.
                Thread.sleep(300);

                List<KcisaXml.Event> events = kcisaClient.searchEvents(dtype, null, 1, 20)
                        .blockOptional()
                        .orElse(Collections.emptyList());

                if (!events.isEmpty()) {
                    bucket.addAll(events);
                }
            } catch (InterruptedException e) {
                log.warn("Thread sleep interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt(); // 스레드 인터럽트 상태 복원
            } catch (Exception e) {
                log.error("Error fetching events for dtype {}: {}", dtype, e.getMessage());
            }
        }

        if (bucket.isEmpty()) {
            return new Slice<>(Collections.emptyList(), 0, page, size, sort, dir);
        }

        var filtered = bucket.stream()
                .filter(Objects::nonNull) // 혹시 모를 null 값 제거
                .distinct()
                .filter(e -> {
                    String p = e.getPeriod() != null ? e.getPeriod() : e.getEventPeriod();
                    return PeriodParser.parse(p)
                            .map(r -> !r.end().isBefore(today) && !r.start().isAfter(until))
                            .orElse(true);
                })
                .toList();

        Comparator<KcisaXml.Event> cmp = Comparator.comparing(
                e -> PeriodParser.parse(e.getPeriod() != null ? e.getPeriod() : e.getEventPeriod())
                        .map(DateRange::start)
                        .orElse(LocalDate.MAX)
        );
        if ("views".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparingInt(e -> {
                try { return Integer.parseInt(e.getViewCount() == null ? "0" : e.getViewCount().trim()); }
                catch (Exception ex) { return 0; }
            });
        }
        if ("desc".equalsIgnoreCase(dir)) cmp = cmp.reversed();

        var sorted = filtered.stream().sorted(cmp).toList();

        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        var content = sorted.subList(from, to);

        return new Slice<>(content, sorted.size(), page, size, sort, dir);
    }

    public List<KcisaXml.Event> search(String dtype, String title, int pageNo, int rows) {
        return kcisaClient.searchEvents(dtype, title, pageNo, rows)
                .blockOptional()
                .orElse(Collections.emptyList());
    }

    public record Slice<T>(List<T> content, int total, int page, int size, String sort, String dir) {}
}