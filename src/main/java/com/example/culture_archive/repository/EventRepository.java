package com.example.culture_archive.repository;

import com.example.culture_archive.domain.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByTitleAndPeriod(String title, String period);
    List<Event> findByEndDateAfterOrEndDateIsNull(LocalDate date);
    Optional<Event> findByTitle(String title);
    List<Event> findByTitleContainingIgnoreCase(String keyword);

    @Modifying
    @Query("update Event e set e.viewCount = coalesce(e.viewCount,0) + 1 where e.id = :id")
    int increaseViewCount(@Param("id") Long id);

    // 공백무시 포함 제목 검색
    @Query("""
select e
from Event e
where lower(e.title) like lower(concat('%', :q, '%'))
   or lower(replace(e.title,' ','')) like lower(concat('%', :qNoSpace, '%'))
""")
    List<Event> searchTitleLoose(@Param("q") String q, @Param("qNoSpace") String qNoSpace);

    // 장소 LIKE
    List<Event> findByEventSiteContainingIgnoreCase(String keyword);

    // 통합 검색
    @Query("""
SELECT e FROM Event e
WHERE (:title IS NULL OR :title = '' OR e.title LIKE %:title%)
  AND (:dtype IS NULL OR :dtype = '' OR e.type = :dtype)
  AND (:region IS NULL OR :region = '' OR e.region = :region)
  AND (
        (:onlyOngoing = true AND
            ( (e.startDate IS NULL OR e.startDate <= :today)
              AND (e.endDate IS NULL OR e.endDate >= :today) )
        )
        OR
        (:onlyOngoing = false AND
            ( :includePast = true OR e.endDate IS NULL OR e.endDate >= :today )
        )
      )
""")
    List<Event> searchEvents(@Param("title") String title,
                             @Param("dtype") String dtype,
                             @Param("region") String region,
                             @Param("today") LocalDate today,
                             @Param("includePast") boolean includePast,
                             @Param("onlyOngoing") boolean onlyOngoing);
}
