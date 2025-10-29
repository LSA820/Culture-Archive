package com.example.culture_archive.repository;

import com.example.culture_archive.domain.member.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// src/main/java/com/example/culture_archive/domain/member/FollowRepository.java
public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    List<Follow> findByFolloweeId(Long followeeId);      // 팔로워 목록
    List<Follow> findByFollowerId(Long followerId);      // 내가 팔로우 중
}
