package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.recommendation.RecommendationService;
import com.dku.opensource.be.recommendation.RecommendationService.RecommendedItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * recommend_for_me — 사용자 관심사(임베딩) 기반 개인화 추천. 입력 불필요, userId는 컨텍스트에서 취득.
 * 출력은 search_issues와 동일한 RESULT 형식 → 챗봇 sources(클릭 가능한 이슈)로 노출된다.
 */
@Component
@RequiredArgsConstructor
public class RecommendForMeTool implements AgentTool {

    private static final int LIMIT = 3;

    private final RecommendationService recommendationService;

    @Override public String name() { return "recommend_for_me"; }

    @Override public String description() {
        return "사용자의 관심사에 맞는 법안·청원·입법예고를 추천할 때 사용합니다. 입력은 필요 없습니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String userId = ctx.getUserId();
        if (userId == null || userId.isBlank()) {
            return "추천하려면 로그인이 필요해요.";
        }
        try {
            List<RecommendedItem> items = recommendationService.getRecommendedFeed(userId, LIMIT);
            if (items == null || items.isEmpty()) {
                return "추천 결과 없음 (관심사·프로필이 부족할 수 있어요).";
            }
            StringBuilder sb = new StringBuilder();
            for (RecommendedItem it : items) {
                sb.append(String.format("RESULT:[%s]|ID:%s|TITLE:%s|DDAY:%s%n",
                        it.type(), it.id(), it.title(), calcDDay(it.deadline())));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "추천 생성 실패: " + e.getMessage();
        }
    }

    private String calcDDay(String deadline) {
        if (deadline == null || deadline.isBlank()) return "상시";
        try {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(deadline));
            return days >= 0 ? "D-" + days : "마감";
        } catch (DateTimeParseException e) {
            return "미상";
        }
    }
}
