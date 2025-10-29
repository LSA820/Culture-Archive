package com.example.culture_archive.service;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.repository.EventRepository;
import com.example.culture_archive.external.kcisa.KcisaClient;
import com.example.culture_archive.external.kcisa.KcisaXml;
import com.example.culture_archive.util.RegionClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KcisaSyncService {

    private final KcisaClient kcisaClient;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RegionClassifier regionClassifier;

    private static final List<String> DTYPE_CANDIDATES = List.of(
            "전시", "공연", "뮤지컬", "국악", "연극", "무용", "클래식",
            "오페라", "콘서트", "축제", "교육/체험", "기타"
    );

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void syncEventsFromApi() {
        log.info(">>>> [SCHEDULED] Starting event data synchronization...");

        List<KcisaXml.Event> allApiEvents = new ArrayList<>();
        for (String dtype : DTYPE_CANDIDATES) {
            try {
                Thread.sleep(500);
                kcisaClient.searchEvents(dtype, null, 1, 200)
                        .blockOptional()
                        .ifPresent(allApiEvents::addAll);
            } catch (Exception e) {
                log.error("Error fetching events for dtype {}: {}", dtype, e.getMessage());
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }

        List<Event> newEvents = allApiEvents.stream()
                .map(eventMapper::toEntity)
                .toList();

        int savedCount = 0;
        for (Event event : newEvents) {
            String region = regionClassifier.classify(event.getEventSite());
            event.setRegion(region);

            // type이 비어있을 경우, 제목으로 추론하는 로직 추가
            if (event.getType() == null || event.getType().isBlank()) {
                inferTypeFromTitle(event);
            }

            if (!eventRepository.existsByTitleAndPeriod(event.getTitle(), event.getPeriod())) {
                eventRepository.save(event);
                savedCount++;
            }
        }
        log.info(">>>> [SCHEDULED] Synchronization finished. Added {} new events.", savedCount);
    }

    private void inferTypeFromTitle(Event event) {
        if (event.getTitle() == null) {
            event.setType("기타");
            return;
        }
        String title = event.getTitle().toLowerCase();
        if (title.contains("콘서트")) event.setType("콘서트");
        else if (title.contains("뮤지컬")) event.setType("뮤지컬");
        else if (title.contains("연극")) event.setType("연극");
        else if (title.contains("전시")) event.setType("전시");
        else if (title.contains("축제")) event.setType("축제");
        else if (title.contains("클래식")) event.setType("클래식");
        else if (title.contains("오페라")) event.setType("오페라");
        else if (title.contains("국악")) event.setType("국악");
        else if (title.contains("기타")) event.setType("기타");
        else {
            event.setType("기타");
        }
    }
}