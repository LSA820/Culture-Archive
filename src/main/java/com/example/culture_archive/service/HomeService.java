package com.example.culture_archive.service;

import com.example.culture_archive.external.KcisaXml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final EventService eventService; // KCISA 호출 주입

    public EventService.Slice<KcisaXml.Event> getHomeUpcoming(String sort, String dir, int page, int size) {
        // 전시/공연 병렬 호출 + 합치기 + 정렬 + 슬라이스
        return eventService.getUpcoming(sort, dir, page, size);
    }
}
