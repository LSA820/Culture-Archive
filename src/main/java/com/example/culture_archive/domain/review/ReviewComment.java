// src/main/java/com/example/culture_archive/domain/review/ReviewComment.java
package com.example.culture_archive.domain.review;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewComment {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, length = 200)
    private String authorEmail;

    @Column(nullable = false, length = 2000)
    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Transient
    private boolean mine;


    public ReviewComment(Review review, String authorName, String authorEmail, String content) {
        this.review = review;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.content = content;
    }
}
