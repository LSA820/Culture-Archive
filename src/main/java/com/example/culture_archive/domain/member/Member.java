package com.example.culture_archive.domain.member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 저장 DB Entity
@Entity
@Getter @NoArgsConstructor
@Table(name = "arc_member", uniqueConstraints = @UniqueConstraint(name="uk_member_email", columnNames="email"))
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    public Member(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }
}


