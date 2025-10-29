package com.example.culture_archive.service.keyword;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.domain.keyword.EventKeyword;
import com.example.culture_archive.domain.keyword.Keyword;
import com.example.culture_archive.domain.keyword.KeywordType;
import com.example.culture_archive.domain.keyword.UserKeyword;
import com.example.culture_archive.repository.EventRepository;
import com.example.culture_archive.repository.keyword.EventKeywordRepository;
import com.example.culture_archive.repository.keyword.KeywordRepository;
import com.example.culture_archive.repository.keyword.UserKeywordRepository;
import com.example.culture_archive.service.EventMapper;
import com.example.culture_archive.util.text.AllowedKeywords;
import com.example.culture_archive.web.dto.EventCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final EventKeywordRepository eventKeywordRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFC).toLowerCase();
    }

    @Transactional
    public void seedWhitelistIfEmpty() {
        for (String w : AllowedKeywords.GENRE)
            if (!keywordRepository.existsByNameAndType(w, KeywordType.GENRE))
                keywordRepository.save(new Keyword(w, KeywordType.GENRE, norm(w)));
        for (String w : AllowedKeywords.REGION)
            if (!keywordRepository.existsByNameAndType(w, KeywordType.REGION))
                keywordRepository.save(new Keyword(w, KeywordType.REGION, norm(w)));
        for (String w : AllowedKeywords.AUDIENCE)
            if (!keywordRepository.existsByNameAndType(w, KeywordType.AUDIENCE))
                keywordRepository.save(new Keyword(w, KeywordType.AUDIENCE, norm(w)));
        for (String w : AllowedKeywords.STYLE)
            if (!keywordRepository.existsByNameAndType(w, KeywordType.STYLE))
                keywordRepository.save(new Keyword(w, KeywordType.STYLE, norm(w)));
        for (String w : AllowedKeywords.THEME)
            if (!keywordRepository.existsByNameAndType(w, KeywordType.THEME))
                keywordRepository.save(new Keyword(w, KeywordType.THEME, norm(w)));
        for (String w : AllowedKeywords.VENUE)
            if (!keywordRepository.existsByNameAndType(w, KeywordType.VENUE))
                keywordRepository.save(new Keyword(w, KeywordType.VENUE, norm(w)));
    }

    public List<String> deriveTextsForEvent(Event e) {
        List<String> t = new ArrayList<>();
        if (e == null) return t;
        if (e.getTitle() != null) t.add(e.getTitle());
        if (e.getEventSite() != null) t.add(e.getEventSite());
        if (e.getType() != null) t.add(e.getType());
        if (e.getRegion() != null) t.add(e.getRegion());
        return t;
    }

    /**
     * 이벤트에 키워드가 이미 있으면 그걸 “재사용”해서 유저-키워드만 연결.
     * 없으면 전달된 chosen을 이용해 이벤트·유저 모두 최초 생성.
     */
    @Transactional
    public void assignOnRecord(Long userId, Long eventId, List<String> chosen) {
        // 1) 이벤트에 이미 키워드가 있는가?
        List<Long> eventKwIds = eventKeywordRepository.findKeywordIdsByEventId(eventId);

        if (!eventKwIds.isEmpty()) {
            // 재사용: 유저-키워드만 보장
            for (Long kwId : eventKwIds) {
                userKeywordRepository.findByUserIdAndKeywordId(userId, kwId)
                        .orElseGet(() -> userKeywordRepository.save(new UserKeyword(userId, kwId, 1)));
            }
            return;
        }

        // 2) 최초 생성 경로: chosen 기반으로 이벤트/유저 모두 연결
        if (chosen == null || chosen.isEmpty()) return;

        // 허용 키워드 표준 레코드 찾기
        Map<String, Keyword> pool = keywordRepository.findAll().stream()
                .collect(Collectors.toMap(
                        k -> norm(k.getName()),
                        k -> k,
                        (a, b) -> a
                ));

        for (String name : chosen) {
            if (name == null || name.isBlank()) continue;
            Keyword kw = pool.get(norm(name));
            if (kw == null) continue; // 화이트리스트 밖이면 스킵

            // 이벤트-키워드 링크
            eventKeywordRepository.findByEventIdAndKeywordId(eventId, kw.getId())
                    .orElseGet(() -> eventKeywordRepository.save(new EventKeyword(eventId, kw.getId(), 1)));

            // 유저-키워드 링크
            userKeywordRepository.findByUserIdAndKeywordId(userId, kw.getId())
                    .orElseGet(() -> userKeywordRepository.save(new UserKeyword(userId, kw.getId(), 1)));
        }
    }

    /**
     * 리뷰 삭제 시: 유저-키워드만 정리. 이벤트-키워드는 유지해 재사용.
     */
    @Transactional
    public void cleanupOnReviewDelete(Long userId, Long eventId) {
        List<Long> kwIds = eventKeywordRepository.findKeywordIdsByEventId(eventId);
        if (kwIds.isEmpty()) return;
        userKeywordRepository.deleteByUserIdAndKeywordIdIn(userId, kwIds);
        // eventKeywordRepository.deleteByEventId(eventId); // 삭제하지 않음. 재사용 목적.
    }

    @Transactional(readOnly = true)
    public List<EventCardDto> recommendEventsForUser(Long memberId, Model model) {
        var myKeywords = userKeywordRepository.findByUserId(memberId);
        if (myKeywords.isEmpty()) return List.of();

        var topIds = myKeywords.stream()
                .sorted(Comparator.comparingInt(UserKeyword::getWeight).reversed())
                .limit(5)
                .map(UserKeyword::getKeywordId)
                .toList();

        var topNames = keywordRepository.findAllById(topIds).stream()
                .map(Keyword::getName)
                .toList();
        model.addAttribute("recommendedKeywords", String.join(", ", topNames));

        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var picked = new LinkedHashSet<Event>();

        if (!topIds.isEmpty()) {
            var links = eventKeywordRepository.findAllByKeywordIdIn(topIds);
            if (!links.isEmpty()) {
                var eventIds = links.stream().map(EventKeyword::getEventId).distinct().toList();
                picked.addAll(eventRepository.findAllById(eventIds));
            }
        }
        for (String kw : topNames) {
            if (kw == null || kw.isBlank()) continue;
            picked.addAll(eventRepository.findByTitleContainingIgnoreCase(kw));
            picked.addAll(eventRepository.findByEventSiteContainingIgnoreCase(kw));
        }

        var upcoming = picked.stream()
                .filter(e -> e.getEndDate() == null || !e.getEndDate().isBefore(today))
                .sorted(Comparator
                        .comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Event::getEndDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(12)
                .toList();

        return upcoming.stream().map(eventMapper::toCardDto).toList();
    }
}
