package com.example.culture_archive.service.event;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.web.dto.EventCardDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class EventViewMapper {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DISP = DateTimeFormatter.ofPattern("yy.MM.dd");

    // Event 화면 카드 DTO
    public EventCardDto toCardDto(Event event) {
        String raw = event.getPeriod();
        LocalDate s = event.getStartDate();
        LocalDate ed = event.getEndDate();

        String display = (raw != null && !raw.isBlank())
                ? raw
                : (s == null ? "" : (s.equals(ed) ? DISP.format(s) : DISP.format(s) + " ~ " + DISP.format(ed)));

        String deadline = (ed == null)
                ? ""
                : "마감일 " + DISP.format(ed) + " (D-" + ChronoUnit.DAYS.between(LocalDate.now(KST), ed) + ")";

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

    private String nz(String s) { return s == null ? "" : s; }
}
