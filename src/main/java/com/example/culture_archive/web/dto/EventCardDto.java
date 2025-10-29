package com.example.culture_archive.web.dto;

import java.time.LocalDate;

public record EventCardDto(
        Long id,
        String title,
        String imageUrl,
        String site,
        String url,
        String displayPeriod,   // "25.12.12 ~ 26.01.05" 또는 원문
        String deadlineLabel,   // "마감일 26.01.05 (D-3)" 또는 ""
        LocalDate startDate,    // 정렬/필터용
        LocalDate endDate
) {}