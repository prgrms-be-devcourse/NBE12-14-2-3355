package com.gamelog.nbe121423355.domain.review.controller;

import com.gamelog.nbe121423355.domain.review.service.ReviewCommentService;
import com.gamelog.nbe121423355.domain.review.service.ReviewCommentService.*;
import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}/comments")
@RequiredArgsConstructor
public class ReviewCommentController {
    private final ReviewCommentService service;
    @GetMapping
    public RsData<Page> list(@PathVariable Long reviewId, @RequestParam(required = false) Long parentId,
                            @RequestParam(required = false) Long after, @AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("200-1", "댓글을 조회했습니다.", service.list(reviewId, user == null ? null : user.getId(), parentId, after));
    }
    @GetMapping("/count")
    public RsData<Long> count(@PathVariable Long reviewId) {
        return new RsData<>("200-1", "댓글 수를 조회했습니다.", service.count(reviewId));
    }
    @PostMapping
    public RsData<Item> create(@PathVariable Long reviewId, @AuthenticationPrincipal SecurityUser user, @RequestBody Body body) {
        return new RsData<>("201-1", "댓글을 작성했습니다.", service.create(reviewId, user.getId(), body));
    }
    @GetMapping("/{id}")
    public RsData<Item> get(@PathVariable Long reviewId, @PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("200-1", "댓글을 조회했습니다.", service.get(reviewId, id, user == null ? null : user.getId()));
    }
    @PutMapping("/{id}")
    public RsData<Item> edit(@PathVariable Long reviewId, @PathVariable Long id, @AuthenticationPrincipal SecurityUser user, @RequestBody Body body) {
        return new RsData<>("200-1", "댓글을 수정했습니다.", service.edit(reviewId, id, user.getId(), body));
    }
    @DeleteMapping("/{id}")
    public RsData<Void> delete(@PathVariable Long reviewId, @PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        service.delete(reviewId, id, user.getId());
        return new RsData<>("200-1", "댓글을 삭제했습니다.", null);
    }
    @PutMapping("/{id}/likes")
    public RsData<Item> like(@PathVariable Long reviewId, @PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("200-1", "좋아요를 등록했습니다.", service.like(reviewId, id, user.getId(), true));
    }
    @DeleteMapping("/{id}/likes")
    public RsData<Item> unlike(@PathVariable Long reviewId, @PathVariable Long id, @AuthenticationPrincipal SecurityUser user) {
        return new RsData<>("200-1", "좋아요를 취소했습니다.", service.like(reviewId, id, user.getId(), false));
    }
}
