package com.example.culture_diary.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home"; // templates/home.html
    }

    @PostMapping("/signup")
    public String signup(SignupRequest form) {
        log.info("회원가입 요청: username={}, password={}",
                form.getUsername(), form.getPassword());
        return "home";
    }
}

