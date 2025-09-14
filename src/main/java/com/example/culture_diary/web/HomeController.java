package com.example.culture_diary.web;

import com.example.culture_diary.domain.Member;
import com.example.culture_diary.domain.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class HomeController {

    private final MemberRepository memberRepository;

    public HomeController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/")
    public String home() {
        log.info("홈 화면 요청"); // 로그 출력
        return "home";
    }
}
