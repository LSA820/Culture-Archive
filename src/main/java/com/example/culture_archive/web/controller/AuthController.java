package com.example.culture_archive.web.controller;

import com.example.culture_archive.service.member.MemberService;
import com.example.culture_archive.web.dto.LoginForm;
import com.example.culture_archive.web.dto.SignupForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class AuthController {

    private final MemberService memberService;

    // 회원가입 폼 (URL: /member/signup)
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "member/signup";
    }

    // 회원가입 처리 (URL: /member/signup)
    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupForm form, Model model, RedirectAttributes redirectAttributes) {
        try {
            memberService.register(form);
            redirectAttributes.addFlashAttribute("successMessage", "회원가입 완료! 로그인 해주세요.");
            return "redirect:/member/login"; // <-- 2. 리다이렉트 경로도 수정!
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "member/signup";
        }
    }

    // 로그인 폼 (URL: /member/login)
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "member/login";
    }
}