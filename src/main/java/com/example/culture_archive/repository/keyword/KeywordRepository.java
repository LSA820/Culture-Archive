// src/main/java/com/example/culture_archive/domain/keyword/KeywordRepository.java
package com.example.culture_archive.repository.keyword;
import com.example.culture_archive.domain.keyword.Keyword;
import com.example.culture_archive.domain.keyword.KeywordType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByNameAndType(String name, KeywordType type);
    boolean existsByNameAndType(String name, KeywordType type);
}
