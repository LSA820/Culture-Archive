package com.example.culture_archive.web.controller;

import com.example.culture_archive.external.interpark.InterparkCrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CrawlController {

    private final InterparkCrawlService interparkCrawlService;

    // 뮤지컬·전시·콘서트·연극 모두
    @GetMapping("/crawl-interpark/all")
    public String crawlAll() {
        new Thread(() -> interparkCrawlService.crawlAllSelectedGenres()).start();
        return "Interpark all-genre crawling started.";
    }
}
