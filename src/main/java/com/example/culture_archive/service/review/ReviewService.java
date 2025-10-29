package com.example.culture_archive.service.review;

import com.example.culture_archive.domain.event.Event; // Event 엔티티 import
import com.example.culture_archive.repository.EventRepository; // EventRepository import
import com.example.culture_archive.domain.member.Member;
import com.example.culture_archive.repository.MemberRepository;
import com.example.culture_archive.domain.review.Review;
import com.example.culture_archive.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    // 👇 1. EventSyncService 대신 EventRepository를 주입받습니다.
    private final EventRepository eventRepository;

    @Transactional
    public Review createArchive(String userEmail, String eventTitle) {
        Member author = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        Optional<Review> existingReview = reviewRepository.findByAuthorIdAndEventTitle(author.getId(), eventTitle);
        if (existingReview.isPresent()) {
            throw new IllegalStateException("이미 기록된 이벤트입니다.");
        }

        // 👇 2. 캐시를 검색하는 대신, 우리 DB에서 이벤트를 직접 조회합니다.
        Event event = eventRepository.findByTitle(eventTitle)
                .orElseThrow(() -> new NoSuchElementException("DB에서 해당 이벤트를 찾을 수 없습니다: " + eventTitle));

        // 👇 3. DB에서 가져온 Event 엔티티의 정보로 Review를 생성합니다.
        Review newReview = Review.builder()
                .author(author)
                .eventTitle(event.getTitle())
                .eventPosterUrl(event.getImageUrl()) // getImageUrl()로 변경
                .eventPeriod(event.getPeriod())
                .eventPlace(event.getEventSite())
                .isPublic(true)
                .build();

        return reviewRepository.save(newReview);
    }
}