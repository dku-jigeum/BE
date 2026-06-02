package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * explain_recommendation_reason — 이 이슈가 왜 사용자에게 추천됐는지 설명한다.
 * 출력 형식:
 *   REASON:설명 문장
 *   MATCHED_USER:태그1,태그2
 *   MATCHED_EVENT:키워드1,키워드2
 *   SCORE:0.78
 */
@Component
@RequiredArgsConstructor
public class RecommendationReasonTool implements AgentTool {

    private final ExaoneClient exaoneClient;

    @Override public String name() { return "explain_recommendation_reason"; }

    @Override public String description() {
        return "사용자 관심사와 이슈 키워드를 비교해 추천 이유를 설명합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        List<String> userInterests = ctx.getUserInterests();
        List<String> matchedKeywords = ctx.getMatchedKeywords();
        Double score = ctx.getSimilarityScore();

        // matchedKeywords가 없으면 userInterests를 그대로 활용
        List<String> effectiveMatched = (matchedKeywords != null && !matchedKeywords.isEmpty())
                ? matchedKeywords
                : userInterests;

        String interestStr = userInterests.isEmpty() ? "없음" : String.join(", ", userInterests);
        String matchedStr = effectiveMatched.isEmpty() ? "없음" : String.join(", ", effectiveMatched);
        String scoreStr = score != null ? String.format("%.2f", score) : "미제공";

        String prompt = String.format("""
                사용자 관심사: %s
                이슈 제목: %s
                매칭 키워드: %s
                유사도 점수: %s

                위 정보를 바탕으로 아래 형식으로만 출력하세요. 설명 문장 없이 형식만 출력합니다.

                REASON:회원님의 관심사와 연결된 이유 한 문장
                MATCHED_USER:매칭된 사용자 관심 키워드(쉼표 구분)
                MATCHED_EVENT:이슈의 핵심 키워드(쉼표 구분, 최대 4개)
                SCORE:%s""",
                interestStr, ctx.getIssueTitle(), matchedStr, scoreStr, scoreStr
        );

        try {
            return exaoneClient.complete(
                    "개인화 추천 이유를 설명하는 전문가입니다. 지정 형식으로만 답변하세요.",
                    prompt
            );
        } catch (Exception e) {
            return String.format(
                    "REASON:회원님의 관심사인 '%s'와 이 이슈의 키워드가 연결되어 추천되었습니다.%nMATCHED_USER:%s%nMATCHED_EVENT:%s%nSCORE:%s",
                    interestStr, matchedStr, ctx.getIssueTitle(), scoreStr
            );
        }
    }
}
