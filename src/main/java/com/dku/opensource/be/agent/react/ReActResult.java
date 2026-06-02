package com.dku.opensource.be.agent.react;

import lombok.Data;

/**
 * ReAct 루프 실행 결과.
 */
@Data
public class ReActResult {
    private String summary;
    private String impact;
    private String similarIssuesRaw;
    private boolean calendarSuggested;
    private Integer daysLeft;
    private String finalAnswer;
}
