package com.example.culture_archive.service.member;

import com.example.culture_archive.domain.member.Member;
import com.example.culture_archive.repository.MemberRepository;
import com.example.culture_archive.web.dto.SignupForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /* 회원가입: 비밀번호는 반드시 해시 저장 */
    @Transactional
    public Long register(SignupForm form) {
        if (memberRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String encodedPw = passwordEncoder.encode(form.getPassword());
        Member member = new Member(form.getEmail(), form.getUsername(), encodedPw);
        memberRepository.save(member);
        return member.getId();
    }


    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다. id=" + id));
    }

    /* Spring Security가 /login 처리 시 호출하는 메서드 */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member m = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다: " + email));

        // 필요한 경우 DB에 role 필드가 생기면 roles(...) 부분만 바꾸면 됨
        return User.withUsername(m.getEmail())
                .password(m.getPassword())   // BCrypt 해시가 들어있어야 함
                .roles("USER")
                .build();
    }
}
