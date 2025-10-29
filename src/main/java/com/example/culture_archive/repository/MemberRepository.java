    package com.example.culture_archive.repository;

    import com.example.culture_archive.domain.member.Member;
    import org.springframework.data.jpa.repository.JpaRepository;

    import java.util.Optional;

    public interface MemberRepository extends JpaRepository<Member, Long> {
        // 회원가입 중복체크
        boolean existsByEmail(String email);

        // 이메일로 회원 찾기
        Optional<Member> findByEmail(String email);
    }
