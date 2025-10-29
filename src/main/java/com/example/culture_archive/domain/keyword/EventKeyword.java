package com.example.culture_archive.domain.keyword;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "arc_event_keyword",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_arc_event_keyword",
                columnNames = {"event_id", "keyword_id"}
        )
)
public class EventKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(nullable = false)
    private Integer weight = 1;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public EventKeyword(Long eventId, Long keywordId, Integer weight) {
        this.eventId = eventId;
        this.keywordId = keywordId;
        this.weight = (weight != null ? weight : 1);
    }

    /** 이벤트별 최초 키워드 생성 시 호출 */
    public static EventKeyword create(Long eventId, Long keywordId) {
        return new EventKeyword(eventId, keywordId, 1);
    }
}
