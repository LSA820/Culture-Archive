// src/main/java/com/example/culture_archive/domain/review/ReviewCommentRepository.java
package com.example.culture_archive.repository;

import com.example.culture_archive.domain.review.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
}
