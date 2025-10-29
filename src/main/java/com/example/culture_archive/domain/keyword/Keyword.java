// src/main/java/com/example/culture_archive/domain/keyword/Keyword.java
package com.example.culture_archive.domain.keyword;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Getter @NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "arc_keyword",
        uniqueConstraints = @UniqueConstraint(name="uk_arc_keyword_name_type", columnNames={"name","type"}))
public class Keyword {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private KeywordType type;

    @Column(nullable=false, length=120)
    private String normalizedName;

    @CreatedDate
    @Column(updatable=false)
    private LocalDateTime createdAt;

    public Keyword(String name, KeywordType type, String normalizedName) {
        this.name = name;
        this.type = type;
        this.normalizedName = normalizedName;
    }
}
