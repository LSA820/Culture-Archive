package com.example.culture_archive.service.event;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.domain.member.Member;
import com.example.culture_archive.domain.review.Review;
import com.example.culture_archive.repository.EventRepository;
import com.example.culture_archive.repository.ReviewRepository;
import com.example.culture_archive.web.dto.EventCardDto;
import com.example.culture_archive.web.dto.EventDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final ReviewRepository reviewRepository;
    private final EventViewMapper viewMapper;

    public List<EventCardDto> buildHomeCards() {
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<Event> upcomingEvents = eventRepository.findByEndDateAfterOrEndDateIsNull(today);
        return upcomingEvents.stream()
                .map(viewMapper::toCardDto)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(c -> Optional.ofNullable(c.endDate()).orElse(LocalDate.MAX)))
                .toList();
    }

    public List<EventCardDto> buildSearchCards(String title, String dtype, String region,
                                               boolean includePast, boolean onlyOngoing, String sort) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<Event> found = eventRepository.searchEvents(title, dtype, region, today, includePast, onlyOngoing);

        Comparator<Event> comp;
        if ("views".equalsIgnoreCase(sort)) {
            comp = Comparator.comparing((Event e) -> Optional.ofNullable(e.getViewCount()).orElse(0)).reversed();
        } else if ("oldest".equalsIgnoreCase(sort)) {
            comp = Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            comp = Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        }

        return found.stream().sorted(comp).map(viewMapper::toCardDto).toList();
    }

    @Transactional
    public EventDetailDto getEventDetailData(Long eventId, Member currentUser) {
        eventRepository.increaseViewCount(eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다. id=" + eventId));

        List<Review> reviews = reviewRepository.findByEventTitleWithAuthorOrderByCreatedAtDesc(event.getTitle());
        double avg = reviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToDouble(Review::getRating)
                .average().orElse(0.0);

        Long uid = (currentUser != null) ? currentUser.getId() : null;
        return new EventDetailDto(event, reviews, avg, uid);
    }

    /* -------------------------------------------------------------
       OCR + 키워드 기반 검색
    ------------------------------------------------------------- */

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{IsHangul}A-Za-z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "전시","공연","티켓","관람","기록","예매","무료","유료","작품","갤러리","미술관","박물관",
            "페스티벌","축제","콘서트","클래식","뮤지컬","연극","오페라","무용","발레",
            "장소","기간","시간","일시","주최","주관",
            "the","and","with","for","from","this","that","you","your","ticket"
    );

    // 공백무시 검색 보조
    private List<Event> findLoose(String token) {
        if (token == null || token.isBlank()) return List.of();
        String q = token.trim();
        String qNoSpace = q.replaceAll("\\s+", "");
        return eventRepository.searchTitleLoose(q, qNoSpace);
    }

    public List<EventCardDto> findEventsByOcrText(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) return List.of();

        List<String> tokens = Arrays.stream(TOKEN_SPLIT.split(ocrText))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .map(s -> s.length() <= 40 ? s : s.substring(0, 40))
                .toList();
        if (tokens.isEmpty()) return List.of();

        List<String> candidates = tokens.stream()
                .filter(t -> !STOP_WORDS.contains(t.toLowerCase(Locale.ROOT)))
                .toList();
        if (candidates.isEmpty()) candidates = tokens;

        LinkedHashMap<Long, Event> hit = new LinkedHashMap<>();
        Map<Long, Integer> score = new HashMap<>();

        for (String c : candidates) {
            for (Event e : findLoose(c)) {
                hit.putIfAbsent(e.getId(), e);
                score.merge(e.getId(), Math.min(c.replaceAll("\\s+", "").length(), 20), Integer::sum);
            }
        }

        return hit.values().stream()
                .sorted(Comparator.comparingInt((Event e) -> score.getOrDefault(e.getId(), 0)).reversed())
                .limit(12)
                .map(viewMapper::toCardDto)
                .toList();
    }

    public List<EventCardDto> findEventsByAnyTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) return List.of();

        LinkedHashMap<Long, Event> hit = new LinkedHashMap<>();
        Map<Long, Integer> score = new HashMap<>();

        for (String t : terms) {
            for (Event e : findLoose(t)) {
                hit.putIfAbsent(e.getId(), e);
                score.merge(e.getId(), Math.min(t.replaceAll("\\s+", "").length(), 20), Integer::sum);
            }
        }

        return hit.values().stream()
                .sorted(Comparator.comparingInt((Event e) -> score.getOrDefault(e.getId(), 0)).reversed())
                .limit(12)
                .map(viewMapper::toCardDto)
                .toList();
    }

    public String pickBestSearchKeyword(String ocrText) {
        if (ocrText == null) return null;
        List<String> tokens = Arrays.stream(TOKEN_SPLIT.split(ocrText))
                .map(String::trim).filter(s -> s.length() >= 2).toList();
        if (tokens.isEmpty()) return null;

        List<String> candidates = tokens.stream()
                .filter(t -> !STOP_WORDS.contains(t.toLowerCase(Locale.ROOT)))
                .toList();
        if (candidates.isEmpty()) candidates = tokens;

        String best = null;
        int bestCount = -1;
        for (String c : candidates) {
            int count = findLoose(c).size();
            if (count > bestCount) { bestCount = count; best = c; }
        }
        if (best == null || bestCount == 0) {
            best = candidates.stream().max(Comparator.comparingInt(String::length)).orElse(candidates.get(0));
        }
        return best;
    }

    public static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
