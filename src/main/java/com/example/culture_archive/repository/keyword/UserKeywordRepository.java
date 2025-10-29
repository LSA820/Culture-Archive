package com.example.culture_archive.repository.keyword;

import com.example.culture_archive.domain.keyword.UserKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {

    Optional<UserKeyword> findByUserIdAndKeywordId(Long userId, Long keywordId);

    void deleteByUserIdAndKeywordIdIn(Long userId, Collection<Long> keywordIds);

    List<UserKeyword> findByUserId(Long userId);
}
