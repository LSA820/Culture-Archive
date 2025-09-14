package com.example.culture_diary.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String userid);
    Optional<Member> findByEmail(String userid);
}
