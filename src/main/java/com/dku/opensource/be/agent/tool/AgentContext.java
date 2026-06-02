package com.dku.opensource.be.agent.tool;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Tool 실행 시 공유되는 요청 컨텍스트.
 *
 * Planner용 필드 (issueSummary, keyPoints, deadlineType, missingFields,
 * availableFields, profileQuality)는 AgentService.buildContext에서 코드로 계산한다.
 * Tool 실행 단계에서는 issueContent(원문)를 사용한다.
 */
@Getter
@Builder
public class AgentContext {

    // ── 이벤트 ─────────────────────────────────────────────
    private final String issueId;       // billNo / petitionNo / billId
    private final String issueType;     // "bill" | "petition" | "legislation"
    private final String userId;
    private final String issueTitle;
    private final String issueContent;  // Tool 단계용 원문 (길 수 있음)
    private final String deadline;      // ISO date string (nullable)

    // Planner용 압축 필드 (원문 대신 사용)
    private final String issueSummary;  // content 앞 500자 또는 title
    @Singular("keyPoint")
    private final List<String> keyPoints;       // 핵심 포인트 (현재 빈 배열, 추후 확장)
    private final String deadlineType;  // "deadline" | "always_open" | "unknown"

    // ── 추천 근거 ───────────────────────────────────────────
    private final Double similarityScore;
    @Singular("matchedKeyword")
    private final List<String> matchedKeywords;
    @Singular("matchedCategory")
    private final List<String> matchedCategories;

    // ── 사용자 프로필 ────────────────────────────────────────
    @Singular("userInterest")
    private final List<String> userInterests;
    private final String userOccupation;
    private final Integer userAge;

    // Planner용 프로필 상태 (코드에서 계산 — 모델이 서비스 정책을 재판단하지 않도록)
    @Singular("missingField")
    private final List<String> missingFields;   // 누락된 프로필 필드 목록
    @Singular("availableField")
    private final List<String> availableFields; // 존재하는 프로필 필드 목록
    private final String profileQuality;        // "high" | "medium" | "low"
}
