package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ApiResponse<List<FeedItem>> getFeed(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                recommendationService.getRecommendedFeed(userId, limit)
                        .stream().map(FeedItem::from).toList());
    }

    /** 홈 트렌딩 섹션 — 프로필/로그인 불필요. 조회수·참여수 기반 트렌딩 Top-N. */
    @GetMapping("/trending")
    public ApiResponse<List<FeedItem>> getTrending(
            @RequestParam(defaultValue = "9") int limit) {
        return ApiResponse.success(
                recommendationService.getTrendingFeed(limit)
                        .stream().map(FeedItem::from).toList());
    }

    record FeedItem(String id, String type, String title, String content, String linkUrl,
                    String deadline, Integer participantCount, Integer viewCount, String source) {
        static FeedItem from(RecommendationService.RecommendedItem r) {
            return new FeedItem(r.id(), r.type(), r.title(), r.content(), r.linkUrl(),
                    r.deadline(), r.participantCount(), r.viewCount(), r.source());
        }
    }
}
