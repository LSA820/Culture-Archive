package com.example.culture_diary.service;

import com.example.culture_diary.domain.Member;
import com.example.culture_diary.domain.MemberRepository;
import com.example.culture_diary.web.SignupForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public Long register(SignupForm form) {
        if (memberRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        Member member = new Member(form.getEmail(), form.getPassword(), form.getUsername());
        memberRepository.save(member);
        return member.getId();
    }

    public Member authenticate(String email, String password) {
        return memberRepository.findByEmail(email)
                .filter(member -> member.getPassword().equals(password))
                .orElse(null);
    }
}
