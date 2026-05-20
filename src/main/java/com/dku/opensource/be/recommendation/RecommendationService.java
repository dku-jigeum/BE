package com.dku.opensource.be.recommendation;

import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.user.UserProfile;
import com.dku.opensource.be.domain.user.UserProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        String text = buildProfileText(profile);
        if (text.isBlank()) {
            log.info("유저 {} 프로필 정보 없음 — 임베딩 생략", userId);
            return;
        }

        String vector = embeddingService.embed(text);
        userProfileRepository.updateEmbeddingVector(userId, vector);
        log.info("유저 {} 임베딩 업데이트 완료 (텍스트: {})", userId, text);
    }

    private String buildProfileText(UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        if (profile.getAge() != null) {
            sb.append(profile.getAge() / 10 * 10).append("대 ");
        }
        if (profile.getOccupation() != null && !profile.getOccupation().isBlank()) {
            sb.append(profile.getOccupation()).append(". ");
        }
        if (!profile.getInterestTags().isEmpty()) {
            sb.append("관심 분야: ").append(String.join(" ", profile.getInterestTags()));
        }
        return sb.toString().trim();
    }

    public record RecommendedBill(Bill bill, String source) {}

    public List<RecommendedBill> getRecommendedBills(String userId, int limit) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (profile.getEmbeddingVector() == null) {
            log.info("유저 {} 임베딩 벡터 없음 — 인기순 폴백", userId);
            return billRepository.findTop20ByOrderByViewCountDesc().stream()
                    .map(b -> new RecommendedBill(b, "trending"))
                    .toList();
        }

        int personalizedCount = (int) Math.ceil(limit * 0.8);
        int trendingSlots = limit - personalizedCount;

        List<RecommendedBill> personalized = billRepository
                .findByEmbeddingSimilarityAfterDeadline(profile.getEmbeddingVector(), personalizedCount)
                .stream().map(b -> new RecommendedBill(b, "personalized")).toList();

        Set<String> seen = personalized.stream()
                .map(r -> r.bill().getBillNo()).collect(Collectors.toSet());

        List<RecommendedBill> trending = billRepository
                .findTopByViewCountAfterDeadline(trendingSlots + seen.size())
                .stream()
                .filter(b -> !seen.contains(b.getBillNo()))
                .limit(trendingSlots)
                .map(b -> new RecommendedBill(b, "trending"))
                .toList();

        List<RecommendedBill> mixed = new ArrayList<>();
        mixed.addAll(personalized);
        mixed.addAll(trending);
        Collections.shuffle(mixed);
        return mixed;
    }
}
