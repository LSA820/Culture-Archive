package com.example.culture_archive.web.controller;

import com.example.culture_archive.repository.MemberRepository;
import com.example.culture_archive.service.MailService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class FindPasswordController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @GetMapping("/find-password")
    public String form() {
        return "member/find-password-combined";
    }

    /** 단일 엔드포인트: 코드 발송(1단계) + 코드검증/변경(2단계) */
    @PostMapping("/find-password")
    public String handle(@RequestParam String email,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String newPw,
                         @RequestParam(required = false) String confirmPw,
                         HttpSession session,
                         Model model) {

        var opt = memberRepository.findByEmail(email);
        if (opt.isEmpty()) {
            model.addAttribute("errorMessage", "가입된 이메일이 아닙니다.");
            return "member/find-password-combined";
        }

        final String key = "PW_CODE:" + email;

        // 1단계: 코드 발송
        if (code == null || code.isBlank()) {
            String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
            session.setAttribute(key, otp);
            session.setMaxInactiveInterval(60); // 1분
            mailService.sendSimple(email, "[Culture Archive] 비밀번호 변경 인증코드",
                    "인증코드: " + otp + "\n1분 내에 입력하세요.");
            model.addAttribute("successMessage", "인증코드를 이메일로 보냈습니다. 1분 내에 입력해 주세요.");
            model.addAttribute("awaitCode", true);
            model.addAttribute("emailPrefill", email);
            return "member/find-password-combined";
        }

        // 2단계: 코드 검증 + 비번 변경
        String saved = (String) session.getAttribute(key);
        if (saved == null || !saved.equals(code)) {
            model.addAttribute("errorMessage", "인증코드가 올바르지 않거나 만료되었습니다.");
            model.addAttribute("awaitCode", true);
            model.addAttribute("emailPrefill", email);
            return "member/find-password-combined";
        }
        if (newPw == null || newPw.length() < 6 || !newPw.equals(confirmPw)) {
            model.addAttribute("errorMessage", "새 비밀번호 입력이 올바르지 않습니다. 6자 이상이며 확인과 일치해야 합니다.");
            model.addAttribute("awaitCode", true);
            model.addAttribute("emailPrefill", email);
            return "member/find-password-combined";
        }

        var m = opt.get();
        m.setPassword(passwordEncoder.encode(newPw));
        memberRepository.save(m);
        session.removeAttribute(key);
        model.addAttribute("successMessage", "비밀번호가 변경되었습니다. 로그인해 주세요.");
        return "member/find-password-combined";
    }
}
