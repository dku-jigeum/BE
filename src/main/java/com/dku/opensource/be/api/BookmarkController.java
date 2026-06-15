package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.bookmark.UserBookmark;
import com.dku.opensource.be.domain.bookmark.UserBookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관심 이슈(북마크) 엔드포인트. (KAN-42)
 *
 * POST   /api/bookmarks            — 북마크 추가 (이미 있으면 그대로, 멱등)
 * DELETE /api/bookmarks/{eventId}  — 북마크 해제
 * GET    /api/bookmarks            — 내 북마크 목록 (최신순)
 */
@Slf4j
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final UserBookmarkRepository bookmarkRepository;

    @PostMapping
    @Transactional
    public ApiResponse<BookmarkResponse> addBookmark(
            @AuthenticationPrincipal String userId,
            @RequestBody BookmarkRequest req) {

        if (bookmarkRepository.existsByUserIdAndEventId(userId, req.eventId())) {
            return ApiResponse.success(new BookmarkResponse(true, req.eventId(), "이미 담은 이슈입니다."));
        }
        try {
            bookmarkRepository.save(UserBookmark.of(userId, req.eventId(), req.issueType(), req.title()));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 unique 위반 — 이미 담긴 것으로 처리
            return ApiResponse.success(new BookmarkResponse(true, req.eventId(), "이미 담은 이슈입니다."));
        }
        log.info("[Bookmark] 추가 — userId={}, eventId={}", userId, req.eventId());
        return ApiResponse.success(new BookmarkResponse(true, req.eventId(), "관심 이슈에 담았습니다."));
    }

    @DeleteMapping("/{eventId}")
    @Transactional
    public ApiResponse<BookmarkResponse> removeBookmark(
            @AuthenticationPrincipal String userId,
            @PathVariable String eventId) {

        bookmarkRepository.deleteByUserIdAndEventId(userId, eventId);
        log.info("[Bookmark] 해제 — userId={}, eventId={}", userId, eventId);
        return ApiResponse.success(new BookmarkResponse(false, eventId, "관심 이슈에서 제외했습니다."));
    }

    @GetMapping
    public ApiResponse<List<BookmarkItem>> getMyBookmarks(@AuthenticationPrincipal String userId) {
        List<BookmarkItem> items = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookmarkItem::from)
                .toList();
        return ApiResponse.success(items);
    }

    record BookmarkRequest(String eventId, String issueType, String title) {}

    record BookmarkResponse(boolean bookmarked, String eventId, String message) {}

    record BookmarkItem(String eventId, String issueType, String title, String createdAt) {
        static BookmarkItem from(UserBookmark b) {
            return new BookmarkItem(b.getEventId(), b.getIssueType(), b.getTitle(), b.getCreatedAt().toString());
        }
    }
}
