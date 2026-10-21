package com.gamelog.nbe121423355.domain.review.service;

import com.gamelog.nbe121423355.domain.review.entity.*;
import com.gamelog.nbe121423355.domain.review.repository.*;
import com.gamelog.nbe121423355.domain.user.repository.UserRepository;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewCommentService {
    private final ReviewRepository reviews;
    private final ReviewCommentRepository comments;
    private final ReviewCommentLikeRepository likes;
    private final UserRepository users;

    public record Item(Long id, Long parentId, Long userId, String nickname, String profileImageUrl,
                       String content, boolean spoiler, boolean deleted, boolean edited, boolean author,
                       boolean mine, long likeCount, boolean liked, long replyCount, LocalDateTime createdDate) {}
    public record Page(List<Item> comments, Long nextCursor, long totalCount) {}
    public record Body(String content, boolean spoiler, Long parentId) {}

    private Review review(Long id, boolean lock) {
        return (lock ? reviews.findActiveForUpdate(id) : reviews.findActiveById(id))
                .orElseThrow(() -> new ServiceException("404-3", "리뷰를 찾을 수 없습니다."));
    }
    private ReviewComment comment(Long reviewId, Long id, boolean active) {
        return comments.findById(id).filter(c -> c.getReview().getId().equals(reviewId) && (!active || !c.isDeleted()))
                .orElseThrow(() -> new ServiceException("404-20", "댓글을 찾을 수 없습니다."));
    }
    private void owner(ReviewComment c, Long userId) {
        if (!c.getUser().getId().equals(userId)) throw new ServiceException("403-20", "본인의 댓글만 변경할 수 있습니다.");
    }
    private void validate(Body body) {
        if (body.content() == null || body.content().isBlank() || body.content().length() > 1000)
            throw new ServiceException("400-20", "댓글은 공백이 아닌 1~1000자여야 합니다.");
    }
    public Page list(Long reviewId, Long viewer, Long parentId, Long after) {
        var review = review(reviewId, false);
        if (after != null && after < 1) throw new ServiceException("400-20", "잘못된 댓글 커서입니다.");
        if (parentId != null && comment(reviewId, parentId, false).getParent() != null)
            throw new ServiceException("400-20", "원댓글의 답글만 조회할 수 있습니다.");
        int size = parentId == null ? 20 : 10;
        var rows = comments.list(reviewId, parentId, after, PageRequest.of(0, size + 1));
        boolean more = rows.size() > size;
        var visible = rows.subList(0, Math.min(size, rows.size()));
        return new Page(items(review, visible, viewer), more ? visible.getLast().getId() : null,
                comments.countByReview_IdAndDeletedFalse(reviewId));
    }
    public long count(Long reviewId) {
        review(reviewId, false);
        return comments.countByReview_IdAndDeletedFalse(reviewId);
    }
    public Item get(Long reviewId, Long id, Long viewer) {
        var review = review(reviewId, false);
        return items(review, List.of(comment(reviewId, id, false)), viewer).getFirst();
    }
    @Transactional
    public Item create(Long reviewId, Long userId, Body body) {
        var review = review(reviewId, true);
        validate(body);
        var parent = body.parentId() == null ? null : comment(reviewId, body.parentId(), true);
        if (parent != null && parent.getParent() != null) throw new ServiceException("400-20", "답글에는 답글을 달 수 없습니다.");
        var user = users.findById(userId).orElseThrow(() -> new ServiceException("404-4", "사용자를 찾을 수 없습니다."));
        var c = comments.saveAndFlush(new ReviewComment(review, user, body.content(), body.spoiler(), parent));
        return items(review, List.of(c), userId).getFirst();
    }
    @Transactional
    public Item edit(Long reviewId, Long id, Long userId, Body body) {
        var review = review(reviewId, true);
        var c = comment(reviewId, id, true);
        owner(c, userId);
        validate(body);
        c.edit(body.content(), body.spoiler());
        return items(review, List.of(c), userId).getFirst();
    }
    @Transactional
    public void delete(Long reviewId, Long id, Long userId) {
        review(reviewId, true);
        var c = comment(reviewId, id, true);
        owner(c, userId);
        likes.deleteForComment(id);
        c.delete();
    }
    @Transactional
    public Item like(Long reviewId, Long id, Long userId, boolean value) {
        var review = review(reviewId, true);
        var c = comment(reviewId, id, true);
        var key = new ReviewCommentLikeId(userId, id);
        if (value && !likes.existsById(key)) {
            var user = users.findById(userId).orElseThrow(() -> new ServiceException("404-4", "사용자를 찾을 수 없습니다."));
            likes.save(new ReviewCommentLike(c, user));
        } else if (!value) likes.deleteById(key);
        likes.flush();
        return items(review, List.of(c), userId).getFirst();
    }
    @Transactional
    public void purge(Long reviewId) {
        likes.deleteForReview(reviewId);
        comments.deleteReplies(reviewId);
        comments.deleteForReview(reviewId);
    }
    private List<Item> items(Review review, List<ReviewComment> rows, Long viewer) {
        if (rows.isEmpty()) return List.of();
        var ids = rows.stream().map(ReviewComment::getId).toList();
        var counts = likes.counts(ids, viewer).stream().collect(Collectors.toMap(ReviewCommentLikeRepository.Likes::getCommentId, Function.identity()));
        var replies = comments.replyCounts(ids).stream().collect(Collectors.toMap(ReviewCommentRepository.ReplyCount::getParentId, ReviewCommentRepository.ReplyCount::getCount));
        Long author = review.getUserGame().getUser().getId();
        return rows.stream().map(c -> {
            var l = counts.get(c.getId());
            boolean deleted = c.isDeleted();
            return new Item(c.getId(), c.getParent() == null ? null : c.getParent().getId(),
                    deleted ? null : c.getUser().getId(), deleted ? null : c.getUser().getNickname(),
                    deleted ? null : c.getUser().getProfileImageUrl(), deleted ? "" : c.getContent(),
                    !deleted && c.isSpoiler(), deleted, !deleted && c.isEdited(),
                    !deleted && c.getUser().getId().equals(author), !deleted && c.getUser().getId().equals(viewer),
                    deleted || l == null ? 0 : l.getCount(), !deleted && l != null && l.getLiked(),
                    replies.getOrDefault(c.getId(), 0L), c.getCreatedDate());
        }).toList();
    }
}
