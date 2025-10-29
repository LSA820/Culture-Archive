package com.example.culture_archive.web.dto;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.domain.review.Review;
import lombok.Getter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Getter
public class EventDetailDto {
    private final Long id;
    private final String title;
    private final String imageUrl;
    private final String site;
    private final String sourceUrl;
    private final String period;
    private final String deadlineLabel; // 계산된 D-Day 라벨

    private final Double averageRating;
    private final List<Review> reviews;

    private final boolean userHasReviewed;
    private final Long userReviewId;

    // 생성자를 통해 값을 설정하도록 변경 (Builder 대신)
    public EventDetailDto(Event event, List<Review> reviews, Double averageRating, Long currentUserId) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.imageUrl = event.getImageUrl();
        this.site = event.getEventSite();
        this.sourceUrl = event.getSourceUrl();
        this.period = event.getPeriod();

        // 👇 [핵심] 여기서 deadlineLabel을 직접 계산합니다.
        this.deadlineLabel = calculateDeadlineLabel(event.getEndDate());

        this.averageRating = Math.round(averageRating * 10) / 10.0;
        this.reviews = reviews;

        // 현재 사용자가 이 이벤트에 대한 리뷰를 작성했는지 확인
        if (currentUserId != null) {
            Long foundReviewId = reviews.stream()
                    .filter(r -> r.getAuthor() != null && currentUserId.equals(r.getAuthor().getId()))
                    .map(Review::getId)
                    .findFirst()
                    .orElse(null);
            this.userHasReviewed = (foundReviewId != null);
            this.userReviewId = foundReviewId;
        } else {
            this.userHasReviewed = false;
            this.userReviewId = null;
        }
    }

    // D-Day 라벨을 계산하는 헬퍼 메소드
    private String calculateDeadlineLabel(LocalDate endDate) {
        if (endDate == null) {
            return "";
        }
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(ZoneId.of("Asia/Seoul")), endDate);
        if (daysBetween < 0) {
            return "마감";
        }
        return "마감 " + daysBetween + "일 전";
    }
}