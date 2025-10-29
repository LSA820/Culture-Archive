package com.example.culture_archive.web.controller;

import com.example.culture_archive.domain.member.Member;
import com.example.culture_archive.repository.MemberRepository;
import com.example.culture_archive.service.event.EventService;
import com.example.culture_archive.web.dto.EventCardDto;
import com.example.culture_archive.web.dto.EventDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final MemberRepository memberRepository;


    @GetMapping("/search")
    public String search(@RequestParam(required = false) String region,
                         @RequestParam(required = false) String dtype,
                         @RequestParam(required = false) String title,
                         @RequestParam(defaultValue = "false") boolean includePast,
                         @RequestParam(defaultValue = "false") boolean ongoing,
                         @RequestParam(defaultValue = "latest") String sort,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        log.info("[SEARCH] region={}, dtype={}, title='{}', includePast={}, sort={}, page={}", region, dtype, title, includePast, sort, page);

        List<EventCardDto> list = eventService.buildSearchCards(title, dtype, region, includePast, ongoing, sort);

        final int PAGE_SIZE = 10;
        int totalPages = (int) Math.ceil((double) list.size() / PAGE_SIZE);

        int current = Math.max(0, Math.min(page, totalPages > 0 ? totalPages - 1 : 0));

        int from = current * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, list.size());

        List<EventCardDto> content = list.isEmpty() ? List.of() : list.subList(from, to);

        model.addAttribute("events", content);
        model.addAttribute("currentPage", current);
        model.addAttribute("totalPages", totalPages);

        int block = 10;
        int start = (current / block) * block;
        int end = Math.min(start + block - 1, totalPages - 1);
        if (end < start) end = start;

        model.addAttribute("startPage", start);
        model.addAttribute("endPage", end);

        model.addAttribute("region", region);
        model.addAttribute("dtype", dtype);
        model.addAttribute("title", title);
        model.addAttribute("includePast", includePast);
        model.addAttribute("ongoing", ongoing);
        model.addAttribute("sort", sort);

        return "events/search";
    }

    @GetMapping("/{id}")
    public String eventDetail(@PathVariable Long id,
                              @AuthenticationPrincipal User user, // 1. 현재 로그인한 사용자 정보 가져오기
                              Model model) {

        Member currentUser = null;
        // 2. 로그인 상태인지 확인하고, 그렇다면 DB에서 Member 객체를 찾아옵니다.
        if (user != null) {
            currentUser = memberRepository.findByEmail(user.getUsername()).orElse(null);
        }

        // 3. Service를 호출할 때, 찾은 Member 객체를 함께 전달합니다.
        EventDetailDto detailDto = eventService.getEventDetailData(id, currentUser);

        model.addAttribute("eventDetail", detailDto);

        return "events/detail";
    }
}