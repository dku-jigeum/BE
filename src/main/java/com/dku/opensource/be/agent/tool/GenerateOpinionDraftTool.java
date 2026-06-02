package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * generate_opinion_draft_tool — 사용자가 명시적으로 요청했을 때 의견 초안을 생성한다.
 * input 형식: "stance=support|oppose|concern|neutral"
 * 출력 형식:
 *   STANCE:concern
 *   DRAFT:초안 내용
 *   DISCLAIMER:제출 전 반드시 본인의 의견에 맞게 수정하세요.
 */
@Component
@RequiredArgsConstructor
public class GenerateOpinionDraftTool implements AgentTool {

    private final ExaoneClient exaoneClient;

    @Override public String name() { return "generate_opinion_draft"; }

    @Override public String description() {
        return "사용자가 입법예고·청원에 제출할 의견 초안을 생성합니다. 반드시 사용자 클릭 후 실행하세요.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String stance = parseStance(input);
        String interestStr = ctx.getUserInterests().isEmpty()
                ? "일반 시민" : String.join(", ", ctx.getUserInterests());

        String prompt = String.format("""
                이슈 제목: %s
                이슈 내용: %s
                사용자 입장: %s
                사용자 관심사: %s

                위 이슈에 대한 의견 제출용 초안을 2~3문장으로 작성하세요.
                - 법률 자문처럼 단정하지 마세요
                - 특정 정치적 입장을 강요하지 마세요
                - '%s' 입장을 기반으로 작성하세요

                출력 형식:
                STANCE:%s
                DRAFT:의견 초안 내용
                DISCLAIMER:이 초안은 참고용이며, 제출 전 반드시 본인의 의견에 맞게 수정해야 합니다.""",
                ctx.getIssueTitle(),
                ctx.getIssueContent() != null
                        ? ctx.getIssueContent().substring(0, Math.min(500, ctx.getIssueContent().length()))
                        : "본문 없음",
                stance, interestStr, stance, stance
        );

        try {
            return exaoneClient.complete(
                    "정책 의견 초안을 작성하는 전문가입니다. 중립적이고 시민 친화적인 언어로 작성하세요.",
                    prompt
            );
        } catch (Exception e) {
            return String.format(
                    "STANCE:%s%nDRAFT:본 이슈에 대해 '%s' 입장으로 의견을 제출합니다. (초안 생성에 실패했습니다. 직접 작성해 주세요.)%nDISCLAIMER:이 초안은 참고용이며, 제출 전 반드시 본인의 의견에 맞게 수정해야 합니다.",
                    stance, stance
            );
        }
    }

    private String parseStance(String input) {
        if (input == null) return "neutral";
        String lower = input.toLowerCase();
        if (lower.contains("support")) return "support";
        if (lower.contains("oppose")) return "oppose";
        if (lower.contains("concern")) return "concern";
        return "neutral";
    }
}
