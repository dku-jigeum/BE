package com.dku.opensource.be.agent.tool;

/**
 * ReAct 패턴에서 에이전트가 호출할 수 있는 Tool 인터페이스.
 */
public interface AgentTool {
    /** Tool 이름 — ReAct 프롬프트에서 Action으로 참조됨 */
    String name();

    /** Tool 설명 — 시스템 프롬프트에 포함되어 모델이 언제 쓸지 판단 */
    String description();

    /**
     * Tool 실행.
     *
     * @param input Tool에 전달할 입력 (JSON 문자열 또는 단순 텍스트)
     * @param context 공유 컨텍스트 (issueId, userId, issueType 등)
     * @return Observation 문자열
     */
    String run(String input, AgentContext context);
}
