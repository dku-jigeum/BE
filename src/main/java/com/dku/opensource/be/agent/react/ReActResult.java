package com.dku.opensource.be.agent.react;

import lombok.Data;

/**
 * ReAct 루프 실행 결과.
 * 각 툴의 raw 출력을 보관하며, AgentService가 카드 DTO로 변환한다.
 */
@Data
public class ReActResult {
    // 기존 (하위 호환)
    private String summary;
    private String impact;
    private String similarIssuesRaw;
    private boolean calendarSuggested;
    private Integer daysLeft;
    private String finalAnswer;

    // 신규 카드별 raw 출력
    private String recommendationReasonRaw;   // explain_recommendation_reason
    private String impactStructuredRaw;       // impact_analysis_tool (구조화 포맷)
    private String keyDatesRaw;               // extract_key_dates_tool
    private String calendarDecisionRaw;       // decide_calendar_tool
    private String recommendedActionsRaw;     // recommend_user_action_tool
    private String missingProfileQuestionRaw; // ask_missing_profile_tool
}
