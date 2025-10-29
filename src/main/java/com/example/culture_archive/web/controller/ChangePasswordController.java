// ChangePasswordController.java
package com.example.culture_archive.web.controller;

import com.example.culture_archive.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class ChangePasswordController {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/password")
    public String form() { return "member/change-password"; }

    @PostMapping("/password")
    public String change(@RequestParam String current,
                         @RequestParam String next,
                         @RequestParam String confirm,
                         @AuthenticationPrincipal User user,
                         Model model) {
        var me = memberRepository.findByEmail(user.getUsername()).orElseThrow();
        if (!passwordEncoder.matches(current, me.getPassword())) {
            model.addAttribute("err","현재 비밀번호가 올바르지 않습니다."); return "member/change-password";
        }
        if (next.length() < 6 || !next.equals(confirm)) {
            model.addAttribute("err","새 비밀번호 규칙을 확인하세요."); return "member/change-password";
        }
        me.setPassword(passwordEncoder.encode(next));
        memberRepository.save(me);
        model.addAttribute("msg","비밀번호가 변경되었습니다.");
        return "member/change-password";
    }
}
