package com.example.culture_diary.web;

import com.example.culture_diary.service.CultureService;
import com.example.culture_diary.web.KcisaXml;   // ★ 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final CultureService cultureService;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String dtype,
                         @RequestParam(required = false) String title,
                         @RequestParam(required = false) Integer pageNo,
                         @RequestParam(required = false) Integer numOfRows,
                         Model model) {

        List<KcisaXml.Event> events = List.of(); // 기본값
        log.info("KCISA results = {}", events == null ? 0 : events.size());
        model.addAttribute("events", events);
        // dtype, title 둘 다 채워졌을 때만 API 호출
        if (dtype != null && !dtype.isBlank()
                && title != null && title.strip().length() >= 2) {
            events = cultureService.searchEvents(dtype, title, pageNo, numOfRows);
        }

        model.addAttribute("dtype", dtype);
        model.addAttribute("title", title);
        model.addAttribute("events", events);

        return "event/search"; // templates/event/search.html
    }
}
