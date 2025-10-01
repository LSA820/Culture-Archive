package com.example.culture_archive.service;

import com.example.culture_archive.external.KcisaClient;
import com.example.culture_archive.external.KcisaXml;
import com.example.culture_archive.util.PeriodParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.culture_archive.util.DateRange;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EventService {

    private final KcisaClient kcisaClient;

    // 홈에서 오늘~30일 다가오는 일정 반환
    public Slice<KcisaXml.Event> getUpcoming(String sort, String dir, int page, int size) {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(30);

        // API 최소 2자 제약 때문에 기본 키워드 사용
        List<KcisaXml.Event> exhibitions = kcisaClient.searchEvents("전시", "전시", 1, 10);
        List<KcisaXml.Event> performances = kcisaClient.searchEvents("공연", "공연", 1, 10);

        List<KcisaXml.Event> upcoming = Stream.concat(exhibitions.stream(), performances.stream())
                // 변경 (실패해도 일단 포함)
                .filter(e -> {
                    String p = (e.getPeriod() != null ? e.getPeriod() : e.getEventPeriod());
                    return PeriodParser.parse(p)
                            .map(r -> !r.end().isBefore(today) && !r.start().isAfter(until))
                            .orElse(true);              // ← 실패하면 포함
                })
                .toList();

        Comparator<KcisaXml.Event> cmp = Comparator.comparing(
                e -> PeriodParser.parse(
                        e.getPeriod() != null ? e.getPeriod() : e.getEventPeriod()
                ).map(DateRange::start).orElse(LocalDate.MAX)
        );
        if ("views".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparingInt(e -> safeInt(e.getViewCount()));
        }
        if ("desc".equalsIgnoreCase(dir)) cmp = cmp.reversed();

        List<KcisaXml.Event> sorted = upcoming.stream().sorted(cmp).toList();

        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        List<KcisaXml.Event> slice = sorted.subList(from, to);

        return new Slice<>(slice, sorted.size(), page, size, sort, dir);
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s == null ? "0" : s.trim()); }
        catch (Exception e) { return 0; }
    }

    /** 간단한 슬라이스 DTO */
    public record Slice<T>(List<T> content, int total, int page, int size, String sort, String dir) {}
}
