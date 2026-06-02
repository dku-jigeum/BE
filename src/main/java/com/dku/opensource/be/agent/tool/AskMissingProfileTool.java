package com.dku.opensource.be.agent.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ask_missing_profile_tool — 사용자 영향 분석에 필요한 정보가 부족할 때 추가 질문을 생성한다.
 * analyze_user_impact 실행 후 confidence가 낮을 때 조건부 실행한다.
 * 출력 형식:
 *   NEED_INFO:true|false
 *   QUESTION:질문 문장
 *   OPTIONS:옵션1,옵션2,옵션3,답변하지 않음
 *   REASON:이 정보가 필요한 이유
 */
@Component
public class AskMissingProfileTool implements AgentTool {

    @Override public String name() { return "ask_missing_profile_question"; }

    @Override public String description() {
        return "사용자 프로필이 부족할 때 영향 분석 정확도를 높이기 위한 추가 질문을 생성합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        List<String> missingFields = detectMissingFields(ctx);

        if (missingFields.isEmpty()) {
            return "NEED_INFO:false\nQUESTION:\nOPTIONS:\nREASON:";
        }

        // 가장 중요한 누락 필드에 대한 질문 생성
        String field = missingFields.get(0);
        return switch (field) {
            case "occupation" -> """
                    NEED_INFO:true
                    QUESTION:이 정책의 영향을 더 정확히 분석하려면 현재 직업을 알려주세요.
                    OPTIONS:직장인,학생,자영업자,무직,답변하지 않음
                    REASON:직업에 따라 법안의 영향 범위가 달라질 수 있습니다.""";
            case "age" -> """
                    NEED_INFO:true
                    QUESTION:연령대를 알려주시면 더 정확한 분석이 가능해요.
                    OPTIONS:10대,20대,30대,40대 이상,답변하지 않음
                    REASON:연령에 따라 정책 혜택과 의무가 다를 수 있습니다.""";
            case "interests" -> """
                    NEED_INFO:true
                    QUESTION:어떤 분야에 관심이 있으신가요?
                    OPTIONS:환경·에너지,복지·의료,교육,경제·일자리,답변하지 않음
                    REASON:관심 분야를 알면 이슈와의 관련성을 더 잘 분석할 수 있어요.""";
            default -> "NEED_INFO:false\nQUESTION:\nOPTIONS:\nREASON:";
        };
    }

    private List<String> detectMissingFields(AgentContext ctx) {
        List<String> missing = new ArrayList<>();
        if (ctx.getUserOccupation() == null || ctx.getUserOccupation().isBlank()) {
            missing.add("occupation");
        }
        if (ctx.getUserAge() == null) {
            missing.add("age");
        }
        if (ctx.getUserInterests() == null || ctx.getUserInterests().isEmpty()) {
            missing.add("interests");
        }
        return missing;
    }
}
