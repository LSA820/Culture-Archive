package com.example.culture_archive.config;

import com.example.culture_archive.external.interpark.InterparkCrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class CrawlScheduler {

    private final InterparkCrawlService interpark;
    @Scheduled(cron = "0 00 5 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        interpark.crawlAllSelectedGenres();
    }
}
