package com.example.culture_archive.web.controller;

import com.example.culture_archive.service.KcisaSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final KcisaSyncService eventSyncService;

    // "localhost:8080/sync-now" 주소로 접속하면 동기화가 시작됩니다.
    @GetMapping("/sync-now")
    public String manualSync() {
        new Thread(() -> {
            eventSyncService.syncEventsFromApi();
        }).start();

        return "Event data synchronization has been started in the background. It may take several minutes.";
    }
}