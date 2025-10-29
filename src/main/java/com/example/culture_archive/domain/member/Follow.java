package com.example.culture_archive.domain.member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

// src/main/java/com/example/culture_archive/domain/member/Follow.java
@Entity
@Table(name="arc_follow",
        uniqueConstraints=@UniqueConstraint(name="uk_follow_pair", columnNames={"follower_id","followee_id"}))
@Getter
@NoArgsConstructor
public class Follow {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false) Long followerId;
    @Column(nullable=false) Long followeeId;
    @CreatedDate
    @Column(updatable=false)
    LocalDateTime createdAt;
    public Follow(Long fr, Long fe){this.followerId=fr; this.followeeId=fe;}
}

