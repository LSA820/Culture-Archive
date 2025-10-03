package com.example.culture_archive.web.controller;

import com.example.culture_archive.service.EventService;
import com.example.culture_archive.service.HomeService;
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
    private final HomeService homeService;

    /**
     * 홈 첫 진입.
     * - 화면엔 "항상 4장"만 보이도록 size 기본값을 4로 둔다.
     * - 페이징 계산(첫/마지막, 전체 페이지 수)을 여기서 해 템플릿이 단순 반복만 하도록 한다.
     */
    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "date") String sort, // 정렬 기준(date/views/latest)
                       @RequestParam(defaultValue = "asc")  String dir,  // 오름/내림차순
                       @RequestParam(defaultValue = "0")    int page,    // 0부터 시작
                       @RequestParam(defaultValue = "4")    int size,    // 화면엔 4장씩만 노출
                       Model model) {
        var slice = homeService.getHomeUpcoming(sort, dir, Math.max(0,page), size);

        log.info("file.encoding={}, defaultCharset={}",
                System.getProperty("file.encoding"),
                java.nio.charset.Charset.defaultCharset());

        // page 하한 보정(음수 방지)
        int safePage = Math.max(0, page);

        try {
            long total = slice.total();                         // 전체 아이템 수
            int totalPages = (int) ((total + size - 1) / size); // 전체 페이지 수(마지막 = totalPages-1)

            // 데이터 변경 등으로 totalPages가 줄어든 상황 대비: 현재 페이지 상한 보정
            int current = Math.min(safePage, Math.max(totalPages - 1, 0));

            boolean isFirst = (current == 0);
            boolean isLast  = (totalPages == 0) || (current >= totalPages - 1);

            // 화면 바인딩
            model.addAttribute("events", slice.content());
            model.addAttribute("total", total);
            model.addAttribute("page", current);
            model.addAttribute("size", size);
            model.addAttribute("sort", sort);
            model.addAttribute("dir",  dir);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("isFirst", isFirst);
            model.addAttribute("isLast",  isLast);

        } catch (Exception e) {
            // 실패 시에도 화면은 뜨도록 안전값 주입
            log.warn("홈 일정 로딩 실패: {}", e.getMessage());
            model.addAttribute("events", List.of());
            model.addAttribute("total", 0L);
            model.addAttribute("page", 0);
            model.addAttribute("size", size);
            model.addAttribute("sort", sort);
            model.addAttribute("dir",  dir);
            model.addAttribute("totalPages", 0);
            model.addAttribute("isFirst", true);
            model.addAttribute("isLast",  true);
        }

        // (지금은) 후기 더미. 실제 서비스 연동 시 교체
        model.addAttribute("reviews", List.of());
        return "home";
    }

    /**
     * htmx로 "다가오는 일정" 카드 영역만 부분 렌더링.
     * - 프론트에서 화살표/정렬이 바뀔 때 이 엔드포인트로 요청한다.
     * - 반환은 home.html의 프래그먼트:  th:fragment="eventsBox"
     * - 여기서도 size 기본값은 4(홈과 동일한 페이지 크기 유지)
     */
    @GetMapping("/events/box")
    public String eventsBox(@RequestParam(defaultValue = "date") String sort,
                            @RequestParam(defaultValue = "asc")  String dir,
                            @RequestParam(defaultValue = "0")    int page,
                            @RequestParam(defaultValue = "4")    int size,  // ← 4장씩 페이징
                            Model model) {

        var slice = homeService.getHomeUpcoming(sort, dir, Math.max(0,page), size);
        int totalPages = (int) ((slice.total() + size - 1) / size);

        log.info("[eventsBox] sort={},dir={},page={},size={} -> count={}, total={}",
                sort, dir, page, size, slice.content().size(), slice.total());

        model.addAttribute("events", slice.content());
        model.addAttribute("total", slice.total());
        model.addAttribute("page",  slice.page());
        model.addAttribute("size",  slice.size());
        model.addAttribute("sort",  slice.sort());
        model.addAttribute("dir",   slice.dir());
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("isFirst", slice.page() == 0);
        model.addAttribute("isLast",  slice.page() >= Math.max(totalPages - 1, 0));
        model.addAttribute("reviews", java.util.List.of());

        // home.html 안의 <div th:fragment="eventsBox"> ... </div> 영역만 반환
        return "home :: eventsBox";
    }
}
