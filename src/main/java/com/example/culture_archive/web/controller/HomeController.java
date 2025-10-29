package com.example.culture_archive.web.controller;

import com.example.culture_archive.repository.MemberRepository;
import com.example.culture_archive.repository.ReviewRepository;
import com.example.culture_archive.service.event.EventService;
import com.example.culture_archive.service.keyword.KeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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
    private final KeywordService keywordService;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal User user, Model model) {
        // 기본/개인화 카드 셋업
        if (user == null) {
            addDefault(0, 4, model);
            model.addAttribute("isPersonalized", false);
        } else {
            var current = memberRepository.findByEmail(user.getUsername()).orElse(null);
            if (current == null) {
                addDefault(0, 4, model);
                model.addAttribute("isPersonalized", false);
            } else {
                var personalized = keywordService.recommendEventsForUser(current.getId(), model);
                if (personalized == null || personalized.isEmpty()) {
                    addDefault(0, 4, model);
                    model.addAttribute("isPersonalized", false);
                } else {
                    int size = 4, page = 0;
                    int totalPages = (int) Math.ceil(personalized.size() / (double) size);
                    int from = 0, to = Math.min(size, personalized.size());
                    model.addAttribute("events", personalized.subList(from, to));
                    model.addAttribute("page", page);
                    model.addAttribute("size", size);
                    model.addAttribute("totalPages", totalPages);
                    model.addAttribute("isFirst", page == 0);
                    model.addAttribute("isLast", totalPages == 0 || page >= totalPages - 1);
                    model.addAttribute("reviews", List.of());
                    model.addAttribute("isPersonalized", true);
                }
            }
        }

        // 커뮤니티 박스 기본값 항상 주입
        final int cSize = 2;
        var allReviews = reviewRepository.findAllWithAuthorByOrderByCreatedAtDesc();
        int cPage = 0;
        int cTotal = (int) Math.ceil(allReviews.size() / (double) cSize);
        int from = Math.min(cPage * cSize, allReviews.size());
        int to = Math.min(from + cSize, allReviews.size());
        model.addAttribute("reviews", allReviews.subList(from, to));
        model.addAttribute("cPage", cPage);
        model.addAttribute("cTotal", cTotal);

        return "home";
    }

    @GetMapping("/events/box")
    public String eventsBox(@AuthenticationPrincipal User user,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "4") int size,
                            @RequestParam(defaultValue = "default") String mode,
                            Model model) {

        if ("personal".equals(mode) && user != null) {
            var current = memberRepository.findByEmail(user.getUsername()).orElse(null);
            if (current != null) {
                var list = keywordService.recommendEventsForUser(current.getId(), model);
                if (list != null && !list.isEmpty()) {
                    int totalPages = (int) Math.ceil(list.size() / (double) size);
                    int currentPage = Math.min(Math.max(0, page), Math.max(totalPages - 1, 0));
                    int from = Math.min(currentPage * size, list.size());
                    int to = Math.min(from + size, list.size());
                    var content = list.subList(from, to);

                    model.addAttribute("events", content);
                    model.addAttribute("page", currentPage);
                    model.addAttribute("size", size);
                    model.addAttribute("totalPages", totalPages);
                    model.addAttribute("isFirst", currentPage == 0);
                    model.addAttribute("isLast", totalPages == 0 || currentPage >= totalPages - 1);
                    model.addAttribute("reviews", List.of());
                    model.addAttribute("isPersonalized", true);
                    return "home :: eventsBox";
                }
            }
        }

        // 개인화 불가/0건이면 기본
        addDefault(page, size, model);
        model.addAttribute("isPersonalized", false);
        return "home :: eventsBox";
    }

    private void addDefault(int page, int size, Model model) {
        var all = eventService.buildHomeCards();
        int from = Math.min(Math.max(0, page) * size, all.size());
        int to = Math.min(from + size, all.size());
        var content = all.subList(from, to);

        int totalPages = (int) Math.ceil(all.size() / (double) size);
        int current = Math.min(Math.max(0, page), Math.max(totalPages - 1, 0));

        model.addAttribute("events", content);
        model.addAttribute("page", current);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("isFirst", current == 0);
        model.addAttribute("isLast", totalPages == 0 || current >= totalPages - 1);
        model.addAttribute("reviews", List.of());
    }

    @GetMapping("/community/box")
    public String communityBox(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "2") int size,
                               Model model) {
        var all = reviewRepository.findAllWithAuthorByOrderByCreatedAtDesc();
        int total = (int) Math.ceil(all.size() / (double) size);
        int p = Math.min(Math.max(0, page), Math.max(total - 1, 0));
        int from = Math.min(p * size, all.size());
        int to = Math.min(from + size, all.size());
        model.addAttribute("reviews", all.subList(from, to));
        model.addAttribute("cPage", p);
        model.addAttribute("cTotal", total);
        return "home :: communityBox";
    }
}
