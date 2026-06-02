package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.domain.user.UserProfile;
import com.dku.opensource.be.domain.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * impact_analysis_tool — 사용자 프로필(나이·직업·관심사) 기반 영향 분석.
 */
@Component
@RequiredArgsConstructor
public class ImpactAnalysisTool implements AgentTool {

    private final ExaoneClient exaoneClient;
    private final UserProfileRepository userProfileRepository;

    @Override public String name() { return "impact_analysis_tool"; }

    @Override public String description() {
        return "사용자의 나이·직업·관심 분야를 고려해 이 이슈가 사용자에게 미치는 영향을 분석합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String profileDesc = buildProfileDescription(ctx.getUserId());
        String content = ctx.getIssueContent();

        String prompt = String.format("""
                다음 사용자 정보와 법안/청원 내용을 바탕으로, 이 이슈가 사용자에게 미치는 영향을 2~3문장으로 분석해주세요.
                구체적이고 실용적인 내용으로 작성하세요.

                사용자 정보: %s
                이슈 제목: %s
                이슈 내용: %s

                영향 분석:""",
                profileDesc,
                ctx.getIssueTitle(),
                content != null && content.length() > 1000 ? content.substring(0, 1000) + "..." : (content != null ? content : "본문 없음")
        );

        return exaoneClient.complete(
                "당신은 법안·청원이 일반 시민에게 미치는 영향을 쉽게 설명하는 전문가입니다.",
                prompt
        );
    }

    private String buildProfileDescription(String userId) {
        if (userId == null) return "일반 시민";
        return userProfileRepository.findByUserId(userId)
                .map(p -> {
                    StringBuilder sb = new StringBuilder();
                    if (p.getAge() != null) sb.append(p.getAge()).append("세 ");
                    if (p.getOccupation() != null) sb.append(p.getOccupation()).append(" ");
                    if (!p.getInterestTags().isEmpty())
                        sb.append("(관심 분야: ").append(String.join(", ", p.getInterestTags())).append(")");
                    return sb.toString().trim();
                })
                .orElse("일반 시민");
    }
}
