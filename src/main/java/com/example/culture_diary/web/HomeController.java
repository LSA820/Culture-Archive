package com.example.culture_diary.web;

import com.example.culture_diary.domain.Member;
import com.example.culture_diary.domain.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model){
        log.info("홈 화면 요청");
        model.addAttribute("events", null);  // DB 연동 시 채워 넣기
        model.addAttribute("reviews", null); // 커뮤니티 연동 시 채워 넣기
        return "home";
    }
}
