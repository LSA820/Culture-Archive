package com.example.culture_archive.domain.keyword;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "arc_user_keyword",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_arc_user_keyword",
                columnNames = {"user_id", "keyword_id"}
        )
)
public class UserKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(nullable = false)
    private Integer weight = 1;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastUsedAt;

    public UserKeyword(Long userId, Long keywordId, Integer weight) {
        this.userId = userId;
        this.keywordId = keywordId;
        this.weight = (weight != null ? weight : 1);
    }

    /** 이미 존재하는 유저-키워드 관계에 가중치를 올릴 때 */
    public void incrementWeight() {
        this.weight = Math.min(this.weight + 1, 10);
        this.lastUsedAt = LocalDateTime.now();
    }
}
