package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * recommend_user_action_tool — 분석 결과를 바탕으로 사용자가 취할 수 있는 다음 행동을 추천한다.
 * 출력 형식 (여러 줄):
 *   ACTION:view_original|LABEL:법안 원문 보기|REASON:이유|PRIORITY:high
 *   ACTION:save_event|LABEL:관심 이슈 담기|REASON:이유|PRIORITY:medium
 */
@Component
@RequiredArgsConstructor
public class RecommendUserActionTool implements AgentTool {

    private final ExaoneClient exaoneClient;

    @Override public String name() { return "recommend_user_action"; }

    @Override public String description() {
        return "이슈 유형과 사용자 프로필을 바탕으로 사용자가 취할 수 있는 행동 2~3개를 추천합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String issueType = ctx.getIssueType() != null ? ctx.getIssueType().toLowerCase() : "bill";
        boolean hasDeadline = ctx.getDeadline() != null && !ctx.getDeadline().isBlank();
        String interestStr = ctx.getUserInterests().isEmpty()
                ? "없음" : String.join(", ", ctx.getUserInterests());

        String prompt = String.format("""
                이슈 유형: %s
                이슈 제목: %s
                마감일 존재 여부: %s
                사용자 관심사: %s

                위 정보를 바탕으로 사용자가 취할 수 있는 행동 2~3개를 아래 형식으로만 출력하세요.
                사용 가능한 action type: view_original, view_similar_events, calendar_registration, generate_opinion_draft, save_event, share_event

                출력 형식 (한 줄씩):
                ACTION:type|LABEL:버튼 텍스트|REASON:이유 한 문장|PRIORITY:high or medium""",
                issueType, ctx.getIssueTitle(),
                hasDeadline ? "있음 (마감: " + ctx.getDeadline() + ")" : "없음",
                interestStr
        );

        try {
            return exaoneClient.complete(
                    "정책 참여를 돕는 전문가입니다. 지정 형식으로만 행동 추천을 출력하세요.",
                    prompt
            );
        } catch (Exception e) {
            return buildFallback(issueType, hasDeadline);
        }
    }

    private String buildFallback(String issueType, boolean hasDeadline) {
        StringBuilder sb = new StringBuilder();
        sb.append("ACTION:view_original|LABEL:원문 보기|REASON:세부 내용을 직접 확인해보세요.|PRIORITY:high\n");
        if ("legislation".equals(issueType)) {
            sb.append("ACTION:generate_opinion_draft|LABEL:의견 초안 만들기|REASON:이 이슈에 의견을 제출할 수 있어요.|PRIORITY:high\n");
        }
        if (hasDeadline) {
            sb.append("ACTION:calendar_registration|LABEL:캘린더에 등록하기|REASON:마감일을 놓치지 않도록 저장해두세요.|PRIORITY:medium\n");
        }
        sb.append("ACTION:save_event|LABEL:관심 이슈 담기|REASON:추후 진행 상황을 다시 확인하기 좋아요.|PRIORITY:medium");
        return sb.toString();
    }
}
