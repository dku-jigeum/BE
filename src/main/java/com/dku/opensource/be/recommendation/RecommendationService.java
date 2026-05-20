package com.dku.opensource.be.recommendation;

import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.user.UserProfile;
import com.dku.opensource.be.domain.user.UserProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final EmbeddingService embeddingService;
    private final BillRepository billRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void updateUserEmbedding(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (profile.getInterestTags().isEmpty()) {
            log.info("유저 {} 관심 태그 없음 — 임베딩 생략", userId);
            return;
        }

        String text = String.join(" ", profile.getInterestTags());
        String vector = embeddingService.embed(text);
        userProfileRepository.updateEmbeddingVector(userId, vector);
        log.info("유저 {} 임베딩 업데이트 완료 (태그: {})", userId, profile.getInterestTags());
    }

    public List<Bill> getRecommendedBills(String userId, int limit) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (profile.getEmbeddingVector() == null) {
            log.info("유저 {} 임베딩 벡터 없음 — 인기순 폴백", userId);
            return billRepository.findTop20ByOrderByViewCountDesc();
        }

        return billRepository.findByEmbeddingSimilarityAfterDeadline(
                profile.getEmbeddingVector(), limit);
    }
}
