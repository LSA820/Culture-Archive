package com.example.culture_diary.web;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class LoginForm {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}

