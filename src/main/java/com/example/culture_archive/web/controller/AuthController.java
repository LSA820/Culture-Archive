package com.example.culture_archive.web.controller;

import com.example.culture_archive.domain.member.Member;
import com.example.culture_archive.service.MemberService;
import com.example.culture_archive.web.dto.LoginForm;
import com.example.culture_archive.web.dto.SignupForm;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    // 회원가입 폼
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm()); // Thymeleaf 바인딩용
        return "member/signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupForm form, Model model, RedirectAttributes redirectAttributes) {
        try { // 회원가입 성공 시 메시지 + 로그인 화면으로
            memberService.register(form);
            redirectAttributes.addFlashAttribute("successMessage", "회원가입 완료! 로그인 해주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            // 회원가입 실패 -> 메시지 출력
            model.addAttribute("errorMessage", e.getMessage());
            return "member/signup";
        }
    }
    // 로그인 폼
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "member/login";
    }
}
