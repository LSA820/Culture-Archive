package com.example.culture_archive.domain.event;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 이벤트 저장 DB Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String type;          // 전시/공연 등
    private String eventSite;     // 장소

    // 기간(문자열 그대로 보관)
    private String period;

    // 원문 상세/이미지
    private String sourceUrl;     // KCISA의 상세 url
    private String imageUrl;      //  imageObject를 저장

    // 정렬/필터용(선택)
    private Integer viewCount;    // 없으면 null
    private LocalDate startDate;
    private LocalDate endDate;
}
