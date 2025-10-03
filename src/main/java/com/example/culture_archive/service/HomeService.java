package com.example.culture_archive.service;

import com.example.culture_archive.external.KcisaXml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final EventService eventService;

    // 캐시 어노테이션 제거
    public EventService.Slice<KcisaXml.Event> getHomeUpcoming(String sort, String dir, int page, int size) {
        return eventService.getUpcoming(sort, dir, page, size);
    }
}