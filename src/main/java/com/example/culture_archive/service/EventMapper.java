package com.example.culture_archive.service;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.external.kcisa.KcisaXml;
import com.example.culture_archive.util.DateRange;
import com.example.culture_archive.util.PeriodParser;
import com.example.culture_archive.web.dto.EventCardDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public final class EventMapper {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DISP = DateTimeFormatter.ofPattern("yy.MM.dd");


    public Event toEntity(KcisaXml.Event xmlEvent) {
        // period 필드가 비어 있으면 대체 필드(eventPeriod) 사용
        String rawPeriod = coalesce(xmlEvent.getPeriod(), xmlEvent.getEventPeriod());

        // "YYYY.MM.DD ~ YYYY.MM.DD" 형태 등을 파싱해 시작/종료일 도출
        var parsed = PeriodParser.parse(rawPeriod);
        LocalDate startDate = parsed.map(DateRange::start).orElse(null);
        LocalDate endDate = parsed.map(DateRange::end).orElse(null);

        // 엔티티 빌드
        return Event.builder()
                .title(xmlEvent.getTitle())
                .type(xmlEvent.getType())
                .eventSite(xmlEvent.getEventSite())
                .period(rawPeriod)                       // 원문 기간 문자열 보존
                .sourceUrl(xmlEvent.getUrl())
                .imageUrl(xmlEvent.getImageObject())
                .startDate(startDate)                    // 파싱된 시작일
                .endDate(endDate)                        // 파싱된 종료일
                .viewCount(parseInt(xmlEvent.getViewCount())) // 파싱 실패 시 Null
                .build();
    }

    /**
     * 화면 카드용 DTO로 변환.
     * - displayPeriod: 원문이 있으면 그대로, 없으면 날짜 포맷으로 생성
     * - deadlineLabel: 종료일이 있으면 D-값 계산
     */
    public EventCardDto toCardDto(Event event) {
        String raw = event.getPeriod();
        LocalDate s = event.getStartDate();
        LocalDate ed = event.getEndDate();

        // 표시용 기간 문자열 결정
        String display = (raw != null && !raw.isBlank())
                ? raw
                : (s == null
                ? ""
                : s.equals(ed)
                ? DISP.format(s)
                : DISP.format(s) + " ~ " + DISP.format(ed));

        // 마감 라벨 생성(종료일 없는 경우 공백)
        String deadline = (ed == null)
                ? ""
                : "마감일 " + DISP.format(ed) + " (D-" + ChronoUnit.DAYS.between(LocalDate.now(KST), ed) + ")";

        // 카드 DTO 구성
        return new EventCardDto(
                event.getId(),
                nz(event.getTitle()),
                nz(event.getImageUrl()),
                nz(event.getEventSite()),
                nz(event.getSourceUrl()),
                display,
                deadline,
                s, ed
        );
    }

    /** a가 비어 있으면 b를, 둘 다 비어 있으면 null을 반환 */
    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : ((b != null && !b.isBlank()) ? b : null);
    }

    /** null-안전 문자열: null이면 빈 문자열 반환 */
    private String nz(String s) {
        return s == null ? "" : s;
    }

    /**
     * 안전한 정수 변환.
     * - 값이 null/공백/숫자 아님 → null 반환
     * - 정상 숫자 → Integer로 반환
     */
    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
