package com.example.culture_archive.web.controller;

import com.example.culture_archive.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {
    private final EventService eventService;

    @GetMapping("/search")
    public String search(@RequestParam(required=false) String title,
                         @RequestParam(defaultValue="전시") String dtype,
                         @RequestParam(defaultValue="1") int pageNo,
                         @RequestParam(defaultValue="20") int rows,
                         Model model) {
        var list = eventService.search(dtype, title, pageNo, rows); // 예: KCISA 래핑 메서드
        model.addAttribute("events", list);
        return "events/search";
    }
}
