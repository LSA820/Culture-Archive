package com.example.culture_archive.external.kcisa;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.util.DateRange;
import com.example.culture_archive.util.PeriodParser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class KcisaEventMapper {

    public Event toEntity(KcisaXml.Event xmlEvent) {
        String rawPeriod = coalesce(xmlEvent.getPeriod(), xmlEvent.getEventPeriod());
        var parsed = PeriodParser.parse(rawPeriod);
        LocalDate startDate = parsed.map(DateRange::start).orElse(null);
        LocalDate endDate   = parsed.map(DateRange::end).orElse(null);

        return Event.builder()
                .title(xmlEvent.getTitle())
                .type(xmlEvent.getType())
                .eventSite(xmlEvent.getEventSite())
                .period(rawPeriod)
                .sourceUrl(xmlEvent.getUrl())
                .imageUrl(xmlEvent.getImageObject())
                .startDate(startDate)
                .endDate(endDate)
                .viewCount(parseInt(xmlEvent.getViewCount()))
                .build();
    }

    private String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : ((b != null && !b.isBlank()) ? b : null);
    }
    private Integer parseInt(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
    }
}
