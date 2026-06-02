package com.dku.opensource.be.agent.tool;

import lombok.Builder;
import lombok.Getter;

/**
 * Tool 실행 시 공유되는 요청 컨텍스트.
 */
@Getter
@Builder
public class AgentContext {
    private final String issueId;       // billNo / petitionNo / billId
    private final String issueType;     // "bill" | "petition" | "legislation"
    private final String userId;
    private final String issueTitle;
    private final String issueContent;  // 법안 본문
    private final String deadline;      // ISO date string (nullable)
}
