package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * summarize_tool — 법안/청원/입법예고 본문을 3줄로 요약.
 */
@Component
@RequiredArgsConstructor
public class SummarizeTool implements AgentTool {

    private final ExaoneClient exaoneClient;

    @Override public String name() { return "summarize_event"; }

    @Override public String description() {
        return "법안·청원·입법예고의 핵심 내용을 3줄로 요약합니다. 사용자가 빠르게 이슈를 파악할 수 있게 합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String content = ctx.getIssueContent();
        if (content == null || content.isBlank()) {
            return "요약할 본문이 없습니다. 제목: " + ctx.getIssueTitle();
        }

        String prompt = String.format("""
                다음 법안/청원/입법예고 내용을 핵심만 담아 정확히 3줄로 요약해주세요.
                각 줄은 완전한 문장으로 작성하고, 번호나 불릿 없이 줄바꿈으로 구분하세요.

                제목: %s
                내용: %s

                3줄 요약:""",
                ctx.getIssueTitle(),
                content.length() > 2000 ? content.substring(0, 2000) + "..." : content
        );

        return exaoneClient.complete(
                "당신은 법안·청원·입법예고를 시민이 쉽게 이해할 수 있게 요약하는 전문가입니다.",
                prompt
        );
    }
}
