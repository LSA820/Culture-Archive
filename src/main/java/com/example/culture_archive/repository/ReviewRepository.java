package com.example.culture_archive.repository;

import com.example.culture_archive.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 사용자가 이벤트에 남긴 리뷰가 있는지 확인
    Optional<Review> findByAuthorIdAndEventTitle(Long authorId, String eventTitle);

    // 커뮤니티 페이지용
    @Query("SELECT r FROM Review r JOIN FETCH r.author ORDER BY r.createdAt DESC")
    List<Review> findAllWithAuthorByOrderByCreatedAtDesc();

    // 내 피드용
    List<Review> findByAuthorIdOrderByCreatedAtDesc(Long memberId);

    // 이벤트 상세 페이지용 : 해당 제목의 리뷰+작성자 최신순 정렬
    @Query("SELECT r FROM Review r JOIN FETCH r.author WHERE r.eventTitle = :eventTitle ORDER BY r.createdAt DESC")
    List<Review> findByEventTitleWithAuthorOrderByCreatedAtDesc(@Param("eventTitle") String eventTitle);

    // 리뷰 상세 페이지용 : id로 조회하면서 작성자도 즉시 로딩
    @Query("SELECT r FROM Review r JOIN FETCH r.author WHERE r.id = :id")
    Optional<Review> findReviewWithAuthorById(@Param("id") Long id);

    // 커뮤니티 페이지 페이징처리
    @EntityGraph(attributePaths = "author")
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Review> findByEventTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    @Query("""
    SELECT r FROM Review r
    LEFT JOIN FETCH r.author
    WHERE r.id = :id
""")
    Optional<Review> findByIdWithAuthor(@Param("id") Long id);

}