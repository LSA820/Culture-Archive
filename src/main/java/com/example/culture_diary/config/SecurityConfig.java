package com.example.culture_diary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 모든 요청 허용 (개발 초기 단계)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                "/e rror"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                // 로그인/로그아웃 화면 비활성화 (기본 /login 안 보이게)
                .formLogin(login -> login.disable())
                .logout(logout -> logout.disable())
                // HTTP Basic 인증 비활성화
                .httpBasic(basic -> basic.disable())
                // 개발 편의를 위해 CSRF 비활성화 (폼 붙일 때 다시 고려)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // 이후 회원가입/로그인 구현 시 사용할 비밀번호 인코더
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
