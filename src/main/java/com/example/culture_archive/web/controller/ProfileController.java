// src/main/java/com/example/culture_archive/web/controller/ProfileController.java
package com.example.culture_archive.web.controller;

import com.example.culture_archive.domain.member.Follow;
import com.example.culture_archive.repository.FollowRepository;
import com.example.culture_archive.repository.MemberRepository;
import com.example.culture_archive.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final FollowRepository followRepository;

    @GetMapping("/u/{id}")
    public String feed(@PathVariable Long id,
                       @AuthenticationPrincipal User me,
                       Model model) {
        var m = memberRepository.findById(id).orElseThrow();

        Long meId = (me == null) ? null : memberRepository.findByEmail(me.getUsername())
                .map(u -> u.getId()).orElse(null);
        boolean self = meId != null && meId.equals(id);
        boolean following = meId != null && !self &&
                followRepository.existsByFollowerIdAndFolloweeId(meId, id);

        var followers  = followRepository.findByFolloweeId(id); // 나를 구독하는 사람
        var followings = followRepository.findByFollowerId(id); // 내가 구독 중

        var followerUsers = followers.isEmpty()
                ? java.util.List.<com.example.culture_archive.domain.member.Member>of()
                : memberRepository.findAllById(followers.stream().map(f -> f.getFollowerId()).toList());

        var followingUsers = followings.isEmpty()
                ? java.util.List.<com.example.culture_archive.domain.member.Member>of()
                : memberRepository.findAllById(followings.stream().map(f -> f.getFolloweeId()).toList());

        model.addAttribute("user", m);
        model.addAttribute("reviews", reviewRepository.findByAuthorIdOrderByCreatedAtDesc(id));
        model.addAttribute("self", self);
        model.addAttribute("following", following);
        model.addAttribute("followerCount", followers.size());
        model.addAttribute("followingCount", followings.size());
        // 모달용
        model.addAttribute("followerUsers",  followerUsers);
        model.addAttribute("followingUsers", followingUsers);

        return "profile/feed";
    }

    @PostMapping("/u/{id}/follow")
    @Transactional
    public String follow(@PathVariable Long id,
                         @AuthenticationPrincipal User me,
                         @RequestHeader(value = "HX-Request", defaultValue = "false") boolean hx,
                         Model model) {
        if (me != null) {
            var meId = memberRepository.findByEmail(me.getUsername()).map(u -> u.getId()).orElse(null);
            if (meId != null && !meId.equals(id)
                    && !followRepository.existsByFollowerIdAndFolloweeId(meId, id)) {
                followRepository.save(new Follow(meId, id));
            }
        }
        if (hx) { // 버튼만 교체
            model.addAttribute("authorId", id);
            model.addAttribute("following", true);
            return "profile/feed :: followBtn";
        }
        return "redirect:/u/" + id;
    }

    @PostMapping("/u/{id}/unfollow")
    @Transactional
    public String unfollow(@PathVariable Long id,
                           @AuthenticationPrincipal User me,
                           @RequestHeader(value = "HX-Request", defaultValue = "false") boolean hx,
                           Model model) {
        if (me != null) {
            var meId = memberRepository.findByEmail(me.getUsername()).map(u -> u.getId()).orElse(null);
            if (meId != null && !meId.equals(id)) {
                followRepository.deleteByFollowerIdAndFolloweeId(meId, id);
            }
        }
        if (hx) {
            model.addAttribute("authorId", id);
            model.addAttribute("following", false);
            return "profile/feed :: followBtn";
        }
        return "redirect:/u/" + id;
    }


    @GetMapping("/reviews/my-feed")
    public String myFeed(Model model, @AuthenticationPrincipal User user) {
        if (user == null) return "redirect:/member/login";

        var me = memberRepository.findByEmail(user.getUsername()).orElseThrow();

        var myReviews = reviewRepository.findByAuthorIdOrderByCreatedAtDesc(me.getId());
        var followings = followRepository.findByFollowerId(me.getId());
        var followers  = followRepository.findByFolloweeId(me.getId());

        var followingUsers = followings.isEmpty()
                ? java.util.List.<com.example.culture_archive.domain.member.Member>of()
                : memberRepository.findAllById(followings.stream().map(Follow::getFolloweeId).toList());

        var followerUsers = followers.isEmpty()
                ? java.util.List.<com.example.culture_archive.domain.member.Member>of()
                : memberRepository.findAllById(followers.stream().map(Follow::getFollowerId).toList());

        model.addAttribute("username", me.getUsername());
        model.addAttribute("postCount", myReviews.size());
        model.addAttribute("myReviews", myReviews);
        model.addAttribute("followingCount", followings.size());
        model.addAttribute("followerCount",  followers.size());
        model.addAttribute("followingUsers", followingUsers); // 기존
        model.addAttribute("followerUsers",  followerUsers);  // 추가

        return "reviews/my-feed";
    }

}
