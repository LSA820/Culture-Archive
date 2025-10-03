    package com.example.culture_archive.service;

    import com.example.culture_archive.external.KcisaClient;
    import com.example.culture_archive.external.KcisaXml;
    import com.example.culture_archive.util.PeriodParser;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;
    import com.example.culture_archive.util.DateRange;
    import java.util.concurrent.*;
    import java.time.LocalDate;
    import java.util.Comparator;
    import java.util.List;
    import java.util.stream.Stream;

    @Service
    @RequiredArgsConstructor
    public class EventService {

        private final KcisaClient kcisaClient;

        /**
         * 다가오는 일정 조회 (전시 + 공연 합쳐서 반환)
         * @param sort 정렬 기준(date/views)
         * @param dir  asc/desc
         * @param page 페이지 번호
         * @param size 페이지 크기
         */
        public Slice<KcisaXml.Event> getUpcoming(String sort, String dir, int page, int size) {
            LocalDate today = LocalDate.now();
            LocalDate until = today.plusDays(30);

            // 비동기로 전시/공연 병렬 호출
            var exF = CompletableFuture.supplyAsync(() -> kcisaClient.searchEvents("전시", null, 1, 30));
            var pfF = CompletableFuture.supplyAsync(() -> kcisaClient.searchEvents("공연", null, 1, 30));

            // 타임아웃 + 실패 시 빈 리스트
            List<KcisaXml.Event> exhibitions = exF.orTimeout(6, TimeUnit.SECONDS)
                    .exceptionally(e -> List.of())
                    .join();
            List<KcisaXml.Event> performances = pfF.orTimeout(6, TimeUnit.SECONDS)
                    .exceptionally(e -> List.of())
                    .join();

            // 두 리스트 합치고 기간 필터링
            List<KcisaXml.Event> upcoming = Stream.concat(exhibitions.stream(), performances.stream())
                    .filter(e -> {
                        String p = e.getPeriod() != null ? e.getPeriod() : e.getEventPeriod();
                        return PeriodParser.parse(p)
                                .map(r -> !r.end().isBefore(today) && !r.start().isAfter(until))
                                .orElse(true);
                    })
                    .toList();

            // 정렬 기준 설정
            Comparator<KcisaXml.Event> cmp = Comparator.comparing(
                    e -> PeriodParser.parse(e.getPeriod() != null ? e.getPeriod() : e.getEventPeriod())
                            .map(DateRange::start).orElse(LocalDate.MAX)
            );
            if ("views".equalsIgnoreCase(sort)) cmp = Comparator.comparingInt(e -> safeInt(e.getViewCount()));
            if ("desc".equalsIgnoreCase(dir)) cmp = cmp.reversed();

            // 정렬 + 페이지네이션
            List<KcisaXml.Event> sorted = upcoming.stream().sorted(cmp).toList();
            int from = Math.min(page * size, sorted.size());
            int to   = Math.min(from + size, sorted.size());

            return new Slice<>(sorted.subList(from, to), sorted.size(), page, size, sort, dir);
        }

        /** 특정 dtype(title 기반) 검색 */
        public List<KcisaXml.Event> search(String dtype, String title, int pageNo, int rows) {
            return kcisaClient.searchEvents(dtype, title, pageNo, rows);
        }

        private int safeInt(String s) {
            try { return Integer.parseInt(s == null ? "0" : s.trim()); }
            catch (Exception e) { return 0; }
        }

        /** 간단한 슬라이스 DTO */
        public record Slice<T>(List<T> content, int total, int page, int size, String sort, String dir) {}
    }

