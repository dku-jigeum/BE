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
                recommendationService.getRecommendedBills(userId, limit)
                        .stream().map(FeedItem::from).toList());
    }

    record FeedItem(String billNo, String title, String committee, String deadline, int viewCount, String source) {
        static FeedItem from(RecommendationService.RecommendedBill rb) {
            var b = rb.bill();
            return new FeedItem(b.getBillNo(), b.getTitle(), b.getCommittee(),
                    b.getDeadline() != null ? b.getDeadline().toString() : null,
                    b.getViewCount(), rb.source());
        }
    }
}
