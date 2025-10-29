// src/main/java/com/example/culture_archive/web/controller/ReviewController.java
package com.example.culture_archive.web.controller;

import com.example.culture_archive.domain.event.Event;
import com.example.culture_archive.domain.member.Member;
import com.example.culture_archive.domain.review.Review;
import com.example.culture_archive.domain.review.ReviewComment;
import com.example.culture_archive.external.llm.OllamaService;
import com.example.culture_archive.repository.*;
import com.example.culture_archive.service.keyword.KeywordService;
import com.example.culture_archive.util.text.AllowedKeywords;
import com.example.culture_archive.util.text.KeywordExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final KeywordService keywordService;
    private final KeywordExtractor keywordExtractor;
    private final OllamaService ollamaService;
    private final ReviewCommentRepository commentRepository;
    private final FollowRepository followRepository;

    /** 커뮤니티 목록 */
    @GetMapping
    public String communityPage(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "6") int size,
                                @RequestParam(required = false) String keyword,
                                Model model) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Review> p = (keyword != null && !keyword.isBlank())
                ? reviewRepository.findByEventTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword, pageable)
                : reviewRepository.findAllByOrderByCreatedAtDesc(pageable);

        model.addAttribute("reviews", p.getContent());
        model.addAttribute("page", p.getNumber());
        model.addAttribute("totalPages", p.getTotalPages());
        model.addAttribute("size", p.getSize());
        model.addAttribute("keyword", keyword);
        return "reviews/community";
    }

    /** 새 리뷰 작성 폼 — 기존 데이터 있으면 채움 */
    @GetMapping("/new")
    public String newReviewForm(@RequestParam String eventTitle,
                                @RequestParam String eventPlace,
                                @RequestParam String eventPosterUrl,
                                @RequestParam String eventPeriod,
                                Model model,
                                @AuthenticationPrincipal User user) {
        if (user == null) return "redirect:/member/login?redirect=/reviews/new";
        var me = memberRepository.findByEmail(user.getUsername()).orElseThrow();

        Review review = reviewRepository.findByAuthorIdAndEventTitle(me.getId(), eventTitle)
                .orElseGet(() -> Review.builder()
                        .eventTitle(eventTitle)
                        .eventPlace(eventPlace)
                        .eventPosterUrl(eventPosterUrl)
                        .eventPeriod(eventPeriod)
                        .author(me)
                        .build());

        model.addAttribute("review", review);
        model.addAttribute("authorName", me.getUsername());
        model.addAttribute("authorInitial", me.getUsername().isEmpty() ? "U" : me.getUsername().substring(0, 1));
        model.addAttribute("showCommentForm", true);
        model.addAttribute("isMine", true);
        return "reviews/detail";
    }

    /** 리뷰 업서트 + 키워드 갱신 */
    @PostMapping("/new")
    @Transactional
    public String createReview(@ModelAttribute Review review,
                               @AuthenticationPrincipal User user) {
        if (user == null) return "redirect:/member/login";
        Member author = memberRepository.findByEmail(user.getUsername()).orElseThrow();

        Review saved;
        var existing = reviewRepository.findByAuthorIdAndEventTitle(author.getId(), review.getEventTitle());
        if (existing.isPresent()) {
            Review ex = existing.get();
            ex.setEventPlace(review.getEventPlace());
            ex.setEventPosterUrl(review.getEventPosterUrl());
            ex.setEventPeriod(review.getEventPeriod());
            ex.setRating(review.getRating());
            ex.setComment(review.getComment());
            ex.setPublic(true);
            saved = ex; // dirty checking
        } else {
            review.setAuthor(author);
            review.setPublic(true);
            saved = reviewRepository.save(review);
        }

        keywordService.seedWhitelistIfEmpty();
        Event event = eventRepository.findByTitle(saved.getEventTitle()).orElse(null);
        if (event != null) {
            var texts = keywordService.deriveTextsForEvent(event);
            String merged = String.join(" ", texts);
            var chosen = ollamaService.suggestKeywordsFromText(merged, AllowedKeywords.all());
            if (chosen.isEmpty())
                chosen = keywordExtractor.extract(AllowedKeywords.all(), texts.toArray(String[]::new));
            keywordService.assignOnRecord(author.getId(), event.getId(), chosen);
        }

        return "redirect:/reviews/my-feed";
    }

    // 상세 보기
    @GetMapping("/{id}")
    public String reviewDetail(@PathVariable Long id,
                               @RequestParam(value = "from", required = false) String from,
                               Model model,
                               @AuthenticationPrincipal User user) {
        Review review = reviewRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + id));

        Long authorId = (review.getAuthor() != null) ? review.getAuthor().getId() : null;
        String authorName = (review.getAuthor() != null && review.getAuthor().getUsername() != null)
                ? review.getAuthor().getUsername() : "Unknown";
        String authorInitial = authorName.isEmpty() ? "U" : authorName.substring(0, 1);

        var comments = commentRepository.findByReviewIdOrderByCreatedAtAsc(id);
        String meEmail = (user == null) ? null : user.getUsername();
        comments.forEach(c -> c.setMine(meEmail != null && meEmail.equals(c.getAuthorEmail())));

        boolean showCommentForm = !"my-feed".equalsIgnoreCase(from);
        boolean isMine = (user != null
                && review.getAuthor() != null
                && user.getUsername().equals(review.getAuthor().getEmail()));

        model.addAttribute("showCommentForm", showCommentForm);
        model.addAttribute("review", review);
        model.addAttribute("comments", comments);
        model.addAttribute("authorId", authorId);
        model.addAttribute("authorName", authorName);
        model.addAttribute("authorInitial", authorInitial);
        model.addAttribute("isMine", isMine);

        return "reviews/detail";
    }

    /** 평점/본문 갱신 */
    @PostMapping("/{id}/update")
    @Transactional
    public String updateReview(@PathVariable Long id,
                               @RequestParam(required = false) Double rating,
                               @RequestParam(required = false) String comment,
                               @AuthenticationPrincipal User user) {
        Review review = reviewRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + id));
        if (user != null && review.getAuthor() != null && user.getUsername().equals(review.getAuthor().getEmail())) {
            review.setRating(rating);
            review.setComment(comment);
        }
        return "redirect:/reviews/" + id;
    }

    /** 댓글 등록 */
    @PostMapping("/{id}/comments")
    @Transactional
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             @AuthenticationPrincipal User user,
                             HttpServletRequest req,
                             Model model) {
        if (user != null && !content.isBlank()) {
            Review r = reviewRepository.findById(id).orElseThrow();
            Member m = memberRepository.findByEmail(user.getUsername()).orElseThrow();
            commentRepository.save(new ReviewComment(r, m.getUsername(), m.getEmail(), content));
        }
        return "redirect:/reviews/" + id;
    }

    /** 댓글 삭제 */
    @PostMapping("/{rid}/comments/{cid}/delete")
    @Transactional
    public String deleteComment(@PathVariable Long rid,
                                @PathVariable Long cid,
                                @AuthenticationPrincipal User user) {
        commentRepository.findById(cid).ifPresent(c -> {
            if (user != null && user.getUsername().equals(c.getAuthorEmail())) {
                commentRepository.delete(c);
            }
        });
        return "redirect:/reviews/" + rid;
    }
}
