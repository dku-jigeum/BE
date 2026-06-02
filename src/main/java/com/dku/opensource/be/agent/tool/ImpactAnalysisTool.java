package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * impact_analysis_tool — 사용자 프로필(나이·직업·관심사) 기반 영향 분석.
 * 프로필은 AgentContext에 미리 로드되어 있음.
 */
@Component
@RequiredArgsConstructor
public class ImpactAnalysisTool implements AgentTool {

    private final ExaoneClient exaoneClient;

    @Override public String name() { return "analyze_user_impact"; }

    @Override public String description() {
        return "사용자의 나이·직업·관심 분야를 고려해 이 이슈가 사용자에게 미치는 영향을 분석합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String profileDesc = buildProfileDescription(ctx);
        String content = ctx.getIssueContent();

        String prompt = String.format("""
                다음 사용자 정보와 법안/청원 내용을 분석하여 아래 형식으로만 출력하세요.
                형식 외 설명 문장은 절대 출력하지 마세요.

                사용자 정보: %s
                이슈 제목: %s
                이슈 내용: %s

                출력 형식 (각 항목을 줄바꿈으로 구분):
                IMPACT_LEVEL:high|medium|low|unknown
                IMPACT_TYPE:benefit|risk|mixed|neutral|unknown
                SUMMARY:사용자 관점의 1~2문장 요약
                EFFECTS:영향1##영향2##영향3
                UNCERTAINTY:주의사항 또는 한계 한 문장""",
                profileDesc,
                ctx.getIssueTitle(),
                content != null && content.length() > 1000 ? content.substring(0, 1000) + "..." : (content != null ? content : "본문 없음")
        );

        return exaoneClient.complete(
                "당신은 법안·청원이 일반 시민에게 미치는 영향을 분석하는 전문가입니다. 지정된 형식으로만 답변하세요.",
                prompt
        );
    }

    private String buildProfileDescription(AgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        if (ctx.getUserAge() != null) sb.append(ctx.getUserAge()).append("세 ");
        if (ctx.getUserOccupation() != null) sb.append(ctx.getUserOccupation()).append(" ");
        if (!ctx.getUserInterests().isEmpty())
            sb.append("(관심 분야: ").append(String.join(", ", ctx.getUserInterests())).append(")");
        String result = sb.toString().trim();
        return result.isBlank() ? "일반 시민" : result;
    }
}
