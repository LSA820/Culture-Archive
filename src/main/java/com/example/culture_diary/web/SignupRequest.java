package com.example.culture_diary.web;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

@Getter
@Setter
public final class SignupRequest {
    private String username;
    private String password;
}
