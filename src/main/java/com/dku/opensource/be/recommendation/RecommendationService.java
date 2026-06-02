package com.dku.opensource.be.recommendation;

import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
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
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;
    private final UserProfileRepository userProfileRepository;

    public record RecommendedItem(String id, String type, String title, String content, String linkUrl,
                                  String deadline, Integer participantCount, Integer viewCount, String source) {}

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

    public List<RecommendedItem> getRecommendedFeed(String userId, int limit) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        if (profile == null || profile.getEmbeddingVector() == null) {
            log.info("유저 {} 프로필/임베딩 없음 — 트렌딩 폴백", userId);
            return buildTrendingFeed(limit);
        }

        // 80% 개인화: bill 50% + petition 30% + legislation 20%
        int personalizedTotal = (int) Math.ceil(limit * 0.8);
        int billSlot = (int) Math.ceil(personalizedTotal * 0.5);
        int petitionSlot = (int) Math.ceil(personalizedTotal * 0.3);
        int legislationSlot = personalizedTotal - billSlot - petitionSlot;

        String vec = profile.getEmbeddingVector();

        List<RecommendedItem> personalized = new ArrayList<>();

        List<Bill> personalizedBills = billRepository.findByEmbeddingSimilarityAfterDeadline(vec, billSlot);
        if (personalizedBills.isEmpty()) {
            billRepository.findTop20ByOrderByViewCountDesc().stream().limit(billSlot)
                    .forEach(b -> personalized.add(fromBill(b, "trending")));
        } else {
            personalizedBills.forEach(b -> personalized.add(fromBill(b, "personalized")));
        }

        petitionRepository.findByEmbeddingSimilarityAfterDeadline(vec, petitionSlot)
                .forEach(p -> personalized.add(fromPetition(p, "personalized")));
        legislationNoticeRepository.findByEmbeddingSimilarityAfterDeadline(vec, legislationSlot)
                .forEach(l -> personalized.add(fromLegislation(l, "personalized")));

        // 20% 다양성: 마감 임박 (D-30 이내) 3종에서 균등 배분, 중복 제거
        int diversityTotal = limit - personalized.size();
        int eachSlot = Math.max(1, diversityTotal / 3);

        Set<String> seenBills = personalized.stream()
                .filter(r -> "bill".equals(r.type())).map(RecommendedItem::id).collect(Collectors.toSet());
        Set<String> seenPetitions = personalized.stream()
                .filter(r -> "petition".equals(r.type())).map(RecommendedItem::id).collect(Collectors.toSet());
        Set<String> seenLegislation = personalized.stream()
                .filter(r -> "legislation".equals(r.type())).map(RecommendedItem::id).collect(Collectors.toSet());

        List<RecommendedItem> diversity = new ArrayList<>();
        billRepository.findDeadlineUrgent(eachSlot * 3).stream()
                .filter(b -> !seenBills.contains(b.getBillNo())).limit(eachSlot)
                .forEach(b -> diversity.add(fromBill(b, "trending")));
        petitionRepository.findDeadlineUrgent(eachSlot * 3).stream()
                .filter(p -> !seenPetitions.contains(p.getPetitionNo())).limit(eachSlot)
                .forEach(p -> diversity.add(fromPetition(p, "trending")));
        legislationNoticeRepository.findDeadlineUrgent(eachSlot * 3).stream()
                .filter(l -> !seenLegislation.contains(l.getBillId())).limit(eachSlot)
                .forEach(l -> diversity.add(fromLegislation(l, "trending")));

        List<RecommendedItem> mixed = new ArrayList<>();
        mixed.addAll(personalized);
        mixed.addAll(diversity);
        Collections.shuffle(mixed);
        return mixed;
    }

    private List<RecommendedItem> buildTrendingFeed(int limit) {
        int each = Math.max(1, limit / 3);
        List<RecommendedItem> items = new ArrayList<>();
        billRepository.findTop20ByOrderByViewCountDesc().stream().limit(each)
                .forEach(b -> items.add(fromBill(b, "trending")));
        petitionRepository.findTop20ByOrderByParticipantCountDesc().stream().limit(each)
                .forEach(p -> items.add(fromPetition(p, "trending")));
        legislationNoticeRepository.findDeadlineUrgent(each)
                .forEach(l -> items.add(fromLegislation(l, "trending")));
        Collections.shuffle(items);
        return items;
    }

    private static final String PETITION_URL = "https://petitions.assembly.go.kr/status/registered/";

    private RecommendedItem fromBill(Bill b, String source) {
        return new RecommendedItem(b.getBillNo(), "bill", b.getTitle(), b.getContent(), b.getLinkUrl(),
                b.getDeadline() != null ? b.getDeadline().toString() : null,
                null, b.getViewCount(), source);
    }

    private RecommendedItem fromPetition(Petition p, String source) {
        return new RecommendedItem(p.getPetitionNo(), "petition", p.getTitle(), p.getContent(),
                PETITION_URL + p.getPetitionNo(),
                p.getDeadline() != null ? p.getDeadline().toString() : null,
                p.getParticipantCount(), null, source);
    }

    private RecommendedItem fromLegislation(LegislationNotice l, String source) {
        return new RecommendedItem(l.getBillId(), "legislation", l.getTitle(), null, null,
                l.getDeadline() != null ? l.getDeadline().toString() : null,
                null, null, source);
    }
}
