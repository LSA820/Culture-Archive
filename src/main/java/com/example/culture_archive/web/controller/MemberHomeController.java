package com.example.culture_archive.web.controller;

import com.example.culture_archive.domain.member.Member;
import com.example.culture_archive.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MemberHomeController {

    private final MemberService memberService;

    @GetMapping("/myhome")
    public String myHome(HttpSession session, Model model) {
        Long memberId = (Long) session.getAttribute("LOGIN_MEMBER");
        if (memberId == null) return "redirect:/login";

        Member member = memberService.findById(memberId);
        model.addAttribute("member", member);

        return "member/myhome";
    }
}
