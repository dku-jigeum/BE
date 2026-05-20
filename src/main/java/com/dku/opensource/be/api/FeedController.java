package com.dku.opensource.be.api;

import com.dku.opensource.be.common.ApiResponse;
import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ApiResponse<List<FeedItem>> getFeed(
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<Bill> bills = recommendationService.getRecommendedBills(userId, limit);
        return ApiResponse.success(bills.stream().map(FeedItem::from).toList());
    }

    record FeedItem(String billNo, String title, String committee, String deadline, int viewCount) {
        static FeedItem from(Bill b) {
            return new FeedItem(b.getBillNo(), b.getTitle(), b.getCommittee(),
                    b.getDeadline() != null ? b.getDeadline().toString() : null, b.getViewCount());
        }
    }
}
