package com.example.culture_archive.domain.review;

import com.example.culture_archive.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "arc_review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author; // 작성자

    @Column(nullable = false)
    private String eventTitle;      // 이벤트 제목

    private String eventPosterUrl;  // 이벤트 포스터 이미지 URL
    private String eventPeriod;     // 이벤트 기간
    private String eventPlace;      // 이벤트 장소
    private Double rating;          // 별점

    @Lob
    @Column(columnDefinition = "TEXT")
    private String comment;         // 코멘트

    private boolean isPublic;       // 공개 여부 기본값: true

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; // 생성 시각

    @LastModifiedDate
    private LocalDateTime updatedAt; // 수정 시각
}