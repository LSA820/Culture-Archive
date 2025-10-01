package com.example.culture_archive.web.controller;

import com.example.culture_archive.external.KcisaXml;
import com.example.culture_archive.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final EventService eventService;

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "date") String sort,   // 정렬 기준
                       @RequestParam(defaultValue = "asc") String dir,     // asc/desc
                       @RequestParam(defaultValue = "0") int page,         // 0부터 시작
                       @RequestParam(defaultValue = "4") int size,         // 카드 4개
                       Model model) {

        log.info("file.encoding={}, defaultCharset={}",
                System.getProperty("file.encoding"),
                java.nio.charset.Charset.defaultCharset());

        // page 하한 보정
        int safePage = Math.max(0, page);

        try {
            var slice = eventService.getUpcoming(sort, dir, safePage, size);

            long total = slice.total();                // 전체 아이템 수
            int totalPages = (int) ((total + size - 1) / size); // 마지막 페이지 = totalPages-1
            // page 상한 보정 (데이터 변경으로 totalPages가 줄어든 경우 대비)
            int current = Math.min(safePage, Math.max(totalPages - 1, 0));

            // first/last 플래그 계산
            boolean isFirst = (current == 0);
            boolean isLast  = (totalPages == 0) || (current >= totalPages - 1);

            model.addAttribute("events", slice.content());
            model.addAttribute("total", total);
            model.addAttribute("page", current);
            model.addAttribute("size", size);
            model.addAttribute("sort", sort);
            model.addAttribute("dir", dir);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("isFirst", isFirst);
            model.addAttribute("isLast", isLast);

        } catch (Exception e) {
            log.warn("홈 일정 로딩 실패: {}", e.getMessage());
            model.addAttribute("events", List.of());
            model.addAttribute("total", 0L);
            model.addAttribute("page", 0);
            model.addAttribute("size", size);
            model.addAttribute("sort", sort);
            model.addAttribute("dir", dir);
            model.addAttribute("totalPages", 0);
            model.addAttribute("isFirst", true);
            model.addAttribute("isLast", true);
        }

        model.addAttribute("reviews", List.of());
        return "home";
    }
}

