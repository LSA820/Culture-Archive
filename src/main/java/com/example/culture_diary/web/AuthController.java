package com.example.culture_diary.web;

import com.example.culture_diary.domain.Member;
import com.example.culture_diary.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;

    // 1) 회원가입 폼 (GET /signup)
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm()); // Thymeleaf 바인딩용
        return "member/signup";
    }

    // 2) 회원가입 처리 (POST /signup)
    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupForm form) {
        Member member = new Member(form.getEmail(), form.getUsername(), form.getPassword());
        memberRepository.save(member);
        log.info("회원가입 성공: email={}, username={}", form.getEmail(), form.getUsername());
        return "redirect:/login"; // 가입 후 로그인 페이지로
    }

    // (선택) 로그인 폼
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "member/login";
    }
}
